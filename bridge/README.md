# Local aircraft bridge

I keep OpenSky credentials on the computer running this bridge, not in the Android app. The phone connects through an `adb reverse` tunnel. This is a local development tool, not the production backend.

Python 3.10+ is enough. There are no packages to install.

## Start with fixtures

Run from the repository root:

```bash
python -m bridge.server
```

In a second terminal:

```bash
adb devices
adb reverse tcp:8765 tcp:8765
```

Launch AeroDrive and choose **Use OpenSky bridge** on the phone. The server's default response is fictional and must be labeled as a fixture in the app. No OpenSky account or internet access is needed for the fixture source. The street map still needs its own connection; use Test map for the offline layout.

Other scenarios:

```bash
python -m bridge.server --scenario stale
python -m bridge.server --scenario empty
```

Stop the current server with Ctrl+C before starting another scenario. The normal fixture has fixed coordinates with refreshed synthetic timestamps. The app's built-in simulation, not this fixture, is the moving-aircraft test.

## Live OpenSky

OpenSky's [data terms](https://opensky-network.org/about/terms-of-use) require a written agreement for operational REST API use. Open source, free access, and possession of an account are not substitutes for that agreement. Confirm permission for AeroDrive's actual use before enabling this mode.

After permission is in place, copy `.env.example` to `.env` inside this folder. Set the permission flag and the client ID/secret issued through the OpenSky account page. The file is ignored by Git. Existing environment variables take precedence over values loaded from the file.

```bash
python -m bridge.server --live --env-file bridge/.env
```

`OPENSKY_PERMISSION_CONFIRMED=true` records that approval has already been obtained; setting it does not obtain approval. Missing permission or credentials stops startup. Do not commit `.env`, tokens, logs containing credentials, or real data dumps.

The adapter uses [OpenSky's documented OAuth2 client-credentials flow](https://openskynetwork.github.io/opensky-api/rest.html#authentication). It refreshes expiring tokens and retries a rejected token once. There is no username/password or account-rotation fallback.

## Request behavior

All clients share one Austin query and one in-memory cache. The bounds are latitude 29.8 to 30.75 and longitude -98.3 to -97.15. The phone applies its 25-nautical-mile display filter around the fixed downtown origin.

The bridge fetches at most once every 30 seconds during successful use. It honors provider retry headers, delays retries after failures, and has a default local cap of 3,000 state requests per process-day. That counter is in memory: a restart clears it, and it does not know what other clients do with the same account. It is not a replacement for the provider's account quota or a reason to restart to evade limits.

Upstream state timestamps are preserved. A cached response never rewrites a real aircraft's last-position time. On failures the app can retain a previously received position only until the normal 60-second freshness limit expires.

No flight history is persisted. Credentials and aircraft identifiers are not logged by the request handler. OAuth credentials are sent only to the fixed OpenSky token endpoint, and upstream redirects are disabled.

## Local endpoints

Both endpoints require `X-AeroDrive-Client: android-dev` and a loopback Host header. Browser Origin requests are rejected. The header is a development guard, **not authentication**. Other local processes can access this service. Do not forward it to the internet or bind it to a public interface.

```bash
curl -H "X-AeroDrive-Client: android-dev" http://127.0.0.1:8765/health
curl -H "X-AeroDrive-Client: android-dev" http://127.0.0.1:8765/v1/aircraft
```

Use `curl.exe` on Windows where PowerShell aliases `curl`. Arbitrary bounding boxes and upstream URLs are not accepted. The server binds only to `127.0.0.1:8765`.

`/v1/aircraft` returns `source`, the upstream `time` and `states` fields, and local status metadata. `source` is `fixture` or `opensky`. Treat an empty states list as a valid empty result, not a connection failure.

A public backend needs separate authentication, authorization, durable rate accounting, deployment hardening, privacy work, and explicit permission for any redistribution or cross-user caching. None of that is claimed by this local bridge.

## Tests

```bash
python -m unittest discover -s bridge -p 'test_*.py' -v
```

The suite uses fake upstream responses and actual loopback HTTP tests. It covers token reuse and refresh, bounded retry, rate limits, shared cache behavior, malformed responses, fixture isolation, and local request guards. It makes no live OpenSky requests.
