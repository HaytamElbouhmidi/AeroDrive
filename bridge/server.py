from __future__ import annotations

import argparse
import json
import math
import os
import threading
import time
from dataclasses import dataclass, field
from datetime import timezone
from email.utils import parsedate_to_datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any, Callable
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import HTTPRedirectHandler, Request, build_opener

TOKEN_URL = "https://auth.opensky-network.org/auth/realms/opensky-network/protocol/openid-connect/token"
STATES_URL = "https://opensky-network.org/api/states/all"
AUSTIN_BOUNDS = {"lamin": 29.8, "lomin": -98.3, "lamax": 30.75, "lomax": -97.15}
POLL_SECONDS = 30
MAX_BODY_BYTES = 2_000_000


class UpstreamError(Exception):
    def __init__(self, status: int, headers: dict[str, str] | None = None):
        super().__init__(f"Upstream HTTP {status}")
        self.status = status
        self.headers = {key.lower(): value for key, value in (headers or {}).items()}


class BridgeError(Exception):
    def __init__(self, status: int, message: str, retry_after: int = POLL_SECONDS):
        super().__init__(message)
        self.status = status
        self.retry_after = max(POLL_SECONDS, retry_after)


@dataclass(frozen=True)
class Config:
    live: bool = False
    permission_confirmed: bool = False
    client_id: str = field(default="", repr=False)
    client_secret: str = field(default="", repr=False)
    daily_request_cap: int = 3_000

    def validate(self) -> None:
        if not 1 <= self.daily_request_cap <= 3_500:
            raise ValueError("Daily request cap must be between 1 and 3500.")
        if self.live and not self.permission_confirmed:
            raise ValueError("Live mode requires OPENSKY_PERMISSION_CONFIRMED=true after permission is obtained.")
        if self.live and not (self.client_id and self.client_secret):
            raise ValueError("Set OPENSKY_CLIENT_ID and OPENSKY_CLIENT_SECRET on the bridge, not in the app.")


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def http_json(url: str, data: bytes | None, headers: dict[str, str]) -> tuple[dict[str, Any], dict[str, str]]:
    request = Request(url, data=data, headers=headers)
    try:
        with build_opener(NoRedirect()).open(request, timeout=12) as response:
            raw = response.read(MAX_BODY_BYTES + 1)
            if len(raw) > MAX_BODY_BYTES:
                raise ValueError("Upstream response is too large.")
            body = json.loads(raw)
            if not isinstance(body, dict):
                raise ValueError("Upstream response is not a JSON object.")
            return body, {key.lower(): value for key, value in response.headers.items()}
    except HTTPError as error:
        # Do not forward upstream bodies: they can contain account details.
        raise UpstreamError(error.code, dict(error.headers.items())) from None


def retry_seconds(headers: dict[str, str], now: float) -> int:
    lowered = {key.lower(): value for key, value in headers.items()}
    waits = [POLL_SECONDS]
    for key in ("x-rate-limit-retry-after-seconds", "retry-after"):
        value = lowered.get(key)
        if value is None:
            continue
        try:
            number = float(value)
            if math.isfinite(number):
                waits.append(math.ceil(number))
                continue
        except (ValueError, TypeError):
            pass
        if key == "retry-after":
            try:
                date = parsedate_to_datetime(value)
                if date.tzinfo is None:
                    date = date.replace(tzinfo=timezone.utc)
                waits.append(math.ceil(date.timestamp() - now))
            except (ValueError, TypeError, OverflowError):
                pass
    return max(waits)


class OpenSkyClient:
    def __init__(self, config: Config, transport: Callable = http_json,
                 clock: Callable[[], float] = time.monotonic,
                 wall: Callable[[], float] = time.time):
        config.validate()
        self.config = config
        self.transport = transport
        self.clock = clock
        self.wall = wall
        self.token = ""
        self.token_until = 0.0
        self.window_start = clock()
        self.requests = 0

    def _token(self) -> str:
        if self.token and self.clock() < self.token_until:
            return self.token
        body, _ = self.transport(TOKEN_URL, urlencode({
            "grant_type": "client_credentials",
            "client_id": self.config.client_id,
            "client_secret": self.config.client_secret,
        }).encode(), {"Content-Type": "application/x-www-form-urlencoded", "Accept": "application/json"})
        token = body.get("access_token")
        if not isinstance(token, str) or not token:
            raise ValueError("Missing access token.")
        lifetime = float(body.get("expires_in", 1800))
        if not math.isfinite(lifetime) or lifetime <= 0:
            raise ValueError("Invalid access-token lifetime.")
        self.token = token
        self.token_until = self.clock() + max(0, lifetime - min(30, lifetime / 2))
        return token

    def _reserve_request(self) -> None:
        now = self.clock()
        if now - self.window_start >= 86_400:
            self.window_start, self.requests = now, 0
        if self.requests >= self.config.daily_request_cap:
            raise BridgeError(429, "Local daily request cap reached.", math.ceil(86_400 - (now - self.window_start)))
        self.requests += 1

    def fetch(self) -> dict[str, Any]:
        self.config.validate()
        if not self.config.live:
            raise BridgeError(403, "OpenSky requests are disabled.")
        for attempt in range(2):
            try:
                token = self._token()
                self._reserve_request()
                body, _ = self.transport(STATES_URL + "?" + urlencode(AUSTIN_BOUNDS), None,
                    {"Authorization": f"Bearer {token}", "Accept": "application/json",
                     "User-Agent": "AeroDrive/0.2 development-bridge"})
                timestamp = body.get("time")
                states = body.get("states")
                if (not isinstance(timestamp, (int, float)) or isinstance(timestamp, bool)
                        or not math.isfinite(timestamp) or timestamp <= 0
                        or (states is not None and not isinstance(states, list))):
                    raise ValueError("Invalid state-vector response.")
                return {"source": "opensky", "time": timestamp, "states": states or [],
                        "retry_after_seconds": POLL_SECONDS}
            except UpstreamError as error:
                if error.status == 401 and attempt == 0:
                    self.token = ""
                    self.token_until = 0
                    continue
                if error.status == 429:
                    raise BridgeError(429, "OpenSky rate limit reached.",
                                      retry_seconds(error.headers, self.wall())) from None
                if error.status in (401, 403):
                    raise BridgeError(503, "OpenSky authentication or access failed.", 300) from None
                raise BridgeError(502, "OpenSky is unavailable.", 60) from None
            except (URLError, OSError, ValueError, TypeError, OverflowError):
                raise BridgeError(502, "OpenSky request or response failed.", 60) from None
        raise BridgeError(503, "OpenSky authentication failed.", 300)


class TrafficService:
    def __init__(self, config: Config, client: OpenSkyClient | None = None,
                 clock: Callable[[], float] = time.monotonic,
                 wall: Callable[[], float] = time.time, scenario: str = "normal"):
        config.validate()
        if scenario not in ("normal", "stale", "empty"):
            raise ValueError("Unknown fixture scenario.")
        self.config, self.clock, self.wall, self.scenario = config, clock, wall, scenario
        self.client = client or OpenSkyClient(config, clock=clock, wall=wall)
        self.lock = threading.Lock()
        self.cached: dict[str, Any] | None = None
        self.next_request = 0.0
        self.error: BridgeError | None = None
        self.fixture = json.loads((Path(__file__).parent / "fixtures/austin-states.json").read_text())

    def _fixture(self) -> dict[str, Any]:
        now = int(self.wall())
        rows = []
        for stored in self.fixture["states"]:
            row = list(stored)
            row[3] = now - (120 if self.scenario == "stale" else 2)
            row[4] = now
            rows.append(row)
        return {"source": "fixture", "time": now,
                "states": [] if self.scenario == "empty" else rows,
                "retry_after_seconds": POLL_SECONDS}

    def get(self) -> dict[str, Any]:
        with self.lock:
            now = self.clock()
            if now < self.next_request:
                if self.error is not None:
                    raise BridgeError(self.error.status, str(self.error), math.ceil(self.next_request - now))
                if self.cached is not None:
                    return self.cached
            try:
                value = self.client.fetch() if self.config.live else self._fixture()
            except BridgeError as error:
                self.cached = None
                self.error = error
                self.next_request = self.clock() + error.retry_after
                raise
            self.cached = value
            self.error = None
            self.next_request = self.clock() + POLL_SECONDS
            return value


def handler_for(service: TrafficService):
    class Handler(BaseHTTPRequestHandler):
        server_version = "AeroDriveBridge/0.2"
        sys_version = ""

        def do_GET(self):
            allowed_hosts = {f"127.0.0.1:{self.server.server_port}", f"localhost:{self.server.server_port}"}
            if self.headers.get("Host", "") not in allowed_hosts or self.headers.get("Origin"):
                self.reply(403, {"error": "Local development clients only."})
                return
            if self.headers.get("X-AeroDrive-Client") != "android-dev":
                self.reply(403, {"error": "Missing development-client header."})
                return
            if self.path == "/health":
                self.reply(200, {"status": "ok", "source": "opensky" if service.config.live else "fixture"})
                return
            if self.path != "/v1/aircraft":
                self.reply(404, {"error": "Not found."})
                return
            try:
                self.reply(200, service.get())
            except BridgeError as error:
                self.reply(error.status, {"error": str(error)}, error.retry_after)

        def reply(self, status: int, value: dict[str, Any], retry: int | None = None):
            data = json.dumps(value, separators=(",", ":"), allow_nan=False).encode()
            self.send_response(status)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(data)))
            self.send_header("Cache-Control", "no-store")
            self.send_header("X-Content-Type-Options", "nosniff")
            if retry is not None:
                self.send_header("Retry-After", str(retry))
            self.end_headers()
            try:
                self.wfile.write(data)
            except (BrokenPipeError, ConnectionResetError):
                pass

        def log_message(self, format, *args):
            # No request bodies, account values, or flight identifiers in logs.
            return

    return Handler


def read_env(path: Path) -> None:
    for number, raw in enumerate(path.read_text().splitlines(), start=1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        key, separator, value = line.partition("=")
        if not separator or not key.strip().replace("_", "").isalnum():
            raise ValueError(f"Invalid environment-file entry on line {number}.")
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
            value = value[1:-1]
        os.environ.setdefault(key.strip(), value)


def main() -> None:
    parser = argparse.ArgumentParser(description="Local AeroDrive aircraft bridge")
    parser.add_argument("--live", action="store_true", help="Use OpenSky instead of fictional fixtures")
    parser.add_argument("--env-file", type=Path)
    parser.add_argument("--scenario", choices=("normal", "stale", "empty"), default="normal")
    args = parser.parse_args()
    try:
        if args.env_file:
            read_env(args.env_file)
        config = Config(
            live=args.live,
            permission_confirmed=os.getenv("OPENSKY_PERMISSION_CONFIRMED", "").lower() == "true",
            client_id=os.getenv("OPENSKY_CLIENT_ID", "").strip(),
            client_secret=os.getenv("OPENSKY_CLIENT_SECRET", "").strip(),
            daily_request_cap=int(os.getenv("OPENSKY_DAILY_REQUEST_CAP", "3000")),
        )
        service = TrafficService(config, scenario=args.scenario)
    except (ValueError, OSError) as error:
        parser.error(str(error))
    server = ThreadingHTTPServer(("127.0.0.1", 8765), handler_for(service))
    server.daemon_threads = True
    mode = "OpenSky" if config.live else f"fictional fixture ({args.scenario})"
    print(f"AeroDrive bridge: {mode}. Listening on 127.0.0.1:8765. Ctrl+C to stop.")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
