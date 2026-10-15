import io
import json
import threading
import unittest
from concurrent.futures import ThreadPoolExecutor
from http.client import HTTPConnection
from http.server import ThreadingHTTPServer
from unittest.mock import Mock
from urllib.parse import parse_qs, urlparse
from urllib.error import URLError

from bridge.server import (
    AUSTIN_BOUNDS, BridgeError, Config, OpenSkyClient, TrafficService,
    UpstreamError, handler_for, retry_seconds,
)


class Clock:
    def __init__(self):
        self.now = 0.0

    def __call__(self):
        return self.now


def live_config(**values):
    return Config(live=True, permission_confirmed=True, client_id="test-client",
                  client_secret="not-a-real-secret", **values)


def traffic():
    return {"time": 1_800_000_000, "states": []}


class ClientTests(unittest.TestCase):
    def setUp(self):
        self.clock = Clock()
        self.transport = Mock()
        self.client = OpenSkyClient(live_config(), self.transport, self.clock, lambda: 1_800_000_000)

    def test_live_requires_permission(self):
        with self.assertRaises(ValueError):
            Config(live=True, client_id="id", client_secret="secret").validate()

    def test_live_requires_credentials(self):
        with self.assertRaises(ValueError):
            Config(live=True, permission_confirmed=True).validate()

    def test_repr_does_not_expose_credentials(self):
        self.assertNotIn("not-a-real-secret", repr(live_config()))
        self.assertNotIn("test-client", repr(live_config()))

    def test_fixture_client_cannot_fetch_live(self):
        transport = Mock()
        client = OpenSkyClient(Config(), transport)
        with self.assertRaises(BridgeError) as caught:
            client.fetch()
        self.assertEqual(caught.exception.status, 403)
        transport.assert_not_called()

    def test_oauth_and_fixed_bounding_box(self):
        self.transport.side_effect = [({"access_token": "token", "expires_in": 1800}, {}), (traffic(), {})]
        self.client.fetch()
        url, data, headers = self.transport.call_args_list[0].args
        self.assertTrue(url.startswith("https://auth.opensky-network.org/"))
        self.assertEqual(parse_qs(data.decode())["grant_type"], ["client_credentials"])
        url, _, headers = self.transport.call_args_list[1].args
        query = parse_qs(urlparse(url).query)
        self.assertEqual(headers["Authorization"], "Bearer token")
        self.assertEqual(set(query), set(AUSTIN_BOUNDS))
        self.assertLess((AUSTIN_BOUNDS["lamax"] - AUSTIN_BOUNDS["lamin"]) *
                        (AUSTIN_BOUNDS["lomax"] - AUSTIN_BOUNDS["lomin"]), 25)

    def test_token_reused(self):
        self.transport.side_effect = [({"access_token": "token"}, {}), (traffic(), {}), (traffic(), {})]
        self.client.fetch()
        self.client.fetch()
        self.assertEqual(self.transport.call_count, 3)

    def test_token_refreshes_before_expiry(self):
        self.transport.side_effect = [({"access_token": "a", "expires_in": 60}, {}), (traffic(), {}),
                                      ({"access_token": "b", "expires_in": 60}, {}), (traffic(), {})]
        self.client.fetch()
        self.clock.now = 31
        self.client.fetch()
        self.assertEqual(self.transport.call_args.args[2]["Authorization"], "Bearer b")

    def test_401_retries_once(self):
        self.transport.side_effect = [({"access_token": "a"}, {}), UpstreamError(401),
                                      ({"access_token": "b"}, {}), (traffic(), {})]
        self.assertEqual(self.client.fetch()["source"], "opensky")
        self.assertEqual(self.client.requests, 2)

    def test_repeated_401_stops(self):
        self.transport.side_effect = [({"access_token": "a"}, {}), UpstreamError(401),
                                      ({"access_token": "b"}, {}), UpstreamError(401)]
        with self.assertRaises(BridgeError) as caught:
            self.client.fetch()
        self.assertEqual(caught.exception.retry_after, 300)
        self.assertEqual(self.transport.call_count, 4)

    def test_429_honors_provider_header(self):
        self.transport.side_effect = [({"access_token": "a"}, {}),
                                      UpstreamError(429, {"X-Rate-Limit-Retry-After-Seconds": "7200"})]
        with self.assertRaises(BridgeError) as caught:
            self.client.fetch()
        self.assertEqual(caught.exception.retry_after, 7200)

    def test_null_states_become_empty(self):
        self.transport.side_effect = [({"access_token": "a"}, {}), ({"time": 1_800_000_000, "states": None}, {})]
        self.assertEqual(self.client.fetch()["states"], [])

    def test_invalid_states_rejected(self):
        self.transport.side_effect = [({"access_token": "a"}, {}), ({"time": 1_800_000_000, "states": {}}, {})]
        with self.assertRaises(BridgeError):
            self.client.fetch()

    def test_nonfinite_timestamp_rejected(self):
        self.transport.side_effect = [({"access_token": "a"}, {}), ({"time": float("nan"), "states": []}, {})]
        with self.assertRaises(BridgeError):
            self.client.fetch()

    def test_timeout_is_sanitized(self):
        self.transport.side_effect = URLError("not-a-real-secret")
        with self.assertRaises(BridgeError) as caught:
            self.client.fetch()
        self.assertNotIn("not-a-real-secret", str(caught.exception))

    def test_missing_token_rejected(self):
        self.transport.return_value = ({"token_type": "bearer"}, {})
        with self.assertRaises(BridgeError):
            self.client.fetch()

    def test_local_request_cap(self):
        client = OpenSkyClient(live_config(daily_request_cap=1), self.transport, self.clock)
        self.transport.side_effect = [({"access_token": "a"}, {}), (traffic(), {})]
        client.fetch()
        with self.assertRaises(BridgeError) as caught:
            client.fetch()
        self.assertEqual(caught.exception.status, 429)
        self.assertEqual(self.transport.call_count, 2)

    def test_retry_header_defaults_and_rounding(self):
        self.assertEqual(retry_seconds({}, 0), 30)
        self.assertEqual(retry_seconds({"Retry-After": "31.5"}, 0), 32)
        self.assertEqual(retry_seconds({"Retry-After": "nan"}, 0), 30)
        self.assertEqual(retry_seconds({"Retry-After": "1", "X-Rate-Limit-Retry-After-Seconds": "500"}, 0), 500)

    def test_retry_header_http_date(self):
        self.assertEqual(retry_seconds({"Retry-After": "Thu, 01 Jan 1970 00:02:00 GMT"}, 0), 120)


class ServiceTests(unittest.TestCase):
    def setUp(self):
        self.clock = Clock()
        self.client = Mock()
        self.client.fetch.return_value = {"source": "opensky", **traffic()}
        self.service = TrafficService(live_config(), self.client, self.clock, lambda: 1_800_000_000)

    def test_shared_cache(self):
        self.service.get()
        self.clock.now = 29
        self.service.get()
        self.client.fetch.assert_called_once()
        self.clock.now = 30
        self.service.get()
        self.assertEqual(self.client.fetch.call_count, 2)

    def test_concurrent_clients_share_one_request(self):
        with ThreadPoolExecutor(max_workers=8) as pool:
            results = list(pool.map(lambda _: self.service.get(), range(20)))
        self.client.fetch.assert_called_once()
        self.assertTrue(all(result == results[0] for result in results))

    def test_error_cooldown_is_shared(self):
        self.client.fetch.side_effect = BridgeError(429, "Limited", 180)
        for moment in (0, 20, 179):
            self.clock.now = moment
            with self.assertRaises(BridgeError):
                self.service.get()
        self.client.fetch.assert_called_once()
        self.clock.now = 180
        with self.assertRaises(BridgeError):
            self.service.get()
        self.assertEqual(self.client.fetch.call_count, 2)

    def test_fixture_does_not_call_provider(self):
        service = TrafficService(Config(), self.client, self.clock, lambda: 1_800_000_000)
        response = service.get()
        self.assertEqual(response["source"], "fixture")
        self.assertEqual(len(response["states"]), 6)
        self.client.fetch.assert_not_called()

    def test_stale_fixture_keeps_old_position_time(self):
        service = TrafficService(Config(), self.client, self.clock, lambda: 1_800_000_000, "stale")
        row = service.get()["states"][0]
        self.assertEqual(row[3], 1_799_999_880)
        self.assertEqual(row[4], 1_800_000_000)

    def test_empty_fixture(self):
        service = TrafficService(Config(), self.client, self.clock, scenario="empty")
        self.assertEqual(service.get()["states"], [])


class HttpTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        service = TrafficService(Config())
        cls.server = ThreadingHTTPServer(("127.0.0.1", 0), handler_for(service))
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()
        cls.thread.join()

    def request(self, path="/v1/aircraft", headers=None):
        connection = HTTPConnection("127.0.0.1", self.server.server_port, timeout=2)
        connection.request("GET", path, headers=headers or {})
        response = connection.getresponse()
        result = response.status, dict(response.getheaders()), json.loads(response.read())
        connection.close()
        return result

    def test_requires_client_header(self):
        self.assertEqual(self.request()[0], 403)

    def test_accepts_local_app(self):
        status, headers, body = self.request(headers={"X-AeroDrive-Client": "android-dev"})
        self.assertEqual(status, 200)
        self.assertEqual(body["source"], "fixture")
        self.assertEqual(headers["Cache-Control"], "no-store")

    def test_rejects_browser_origin(self):
        status, _, _ = self.request(headers={"X-AeroDrive-Client": "android-dev", "Origin": "https://example.org"})
        self.assertEqual(status, 403)

    def test_rejects_rebinding_host(self):
        status, _, _ = self.request(headers={"X-AeroDrive-Client": "android-dev", "Host": "example.org"})
        self.assertEqual(status, 403)

    def test_does_not_accept_arbitrary_upstream_queries(self):
        status, _, _ = self.request("/v1/aircraft?lamin=0", {"X-AeroDrive-Client": "android-dev"})
        self.assertEqual(status, 404)


if __name__ == "__main__":
    unittest.main()
