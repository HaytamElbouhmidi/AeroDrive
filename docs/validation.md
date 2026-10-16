# Validation record

**Date:** October 15, 2026  
**Scope:** Build-validation add-on for the AeroDrive MapLibre/OpenSky source prototype

## Results

| Check | Result | Evidence or limit |
| --- | --- | --- |
| Validation-runner tests | 33 passed | `validation/evidence/runner-tests.log` |
| Runner syntax | Passed | Both Python files compile to bytecode without syntax errors |
| Real project preflight | Blocked | `validation/evidence/local-preflight.json` |
| Original Kotlin core suite | Not rerun | Earlier handoff reported 45 passing checks |
| Original bridge suite | Not rerun | Earlier handoff reported 29 passing tests |
| Android Gradle compilation | Not run | Full app source and documented Android toolchain unavailable |
| Android lint | Not run | No compiled full project |
| Install on phone | Not run | No connected phone |
| Phone and DHU visual checklist | Not run | No runtime observation or genuine capture |
| Google platform request | Draft only | No authenticated submission |
| Google response/approval | Not received | No case ID or review outcome |

## Source access

The earlier `AeroDrive-MapLibre-OpenSky.zip` was visible in the file Library, but its raw source bytes could not be made available in the active workspace. The latest README and test plan were readable. I used them to match module names, build tasks, ports, test scenarios, and the existing project decisions.

I did not reconstruct or replace the Kotlin app from a prose description. I did not inspect, patch, compile, or rerun the original source. The deliverable is therefore an add-on, not a rebuilt app or an updated complete source archive.

The available local environment had Java 21, not the documented JDK 17 baseline, and no discovered Android SDK or ADB installation. The recorded preflight correctly blocked before invoking Gradle. That result is not an Android compiler failure.

## What the new tests establish

The 33 tests cover Java version parsing, SDK discovery, device selection, debug artifact metadata, APK path containment, subprocess failures/timeouts, source fingerprinting, stale-artifact rejection, separate forward/reverse tunnels, and keeping manual gates open after a mocked build.

The tests use fake SDK values and small ZIP fixtures. They verify the runner, not APK installability, MapLibre rendering, Android Auto behavior, or platform eligibility. A test named `test_mock_build_keeps_manual_gates_open` is explicitly a mock, not a successful Android build.

## Scope of the add-on

I added the build-evidence runner, 33 tests, 30 manual phone/DHU/bridge checks, three primary DHU profiles, an optional cluster probe, a review request draft, and updated documentation.

The runner does not enable release builds, modify the app's host validator, grant OpenSky permissions, send a support request, or upload to Google Play. Its `release_ready` field remains false because this tool does not establish release readiness.

## Open roadmap items

- [ ] Build the Android app and finish the phone/DHU checklist.
- [ ] Get platform feedback on aircraft overlays before treating the car UI as release-ready.

I close the first item only with an actual build and device evidence. I close the feedback item only after a response is recorded and its required changes are understood. Neither condition by itself makes the overall app production-ready.
