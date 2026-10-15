# Validation

October 15, 2026

## Results

| Check | Result |
| --- | --- |
| Kotlin core runner | 45 checks passed |
| Python bridge suite | 29 tests passed |
| Kotlin source/script parsing | 19 files parsed without syntax errors |
| Project static checks | 7 groups passed |
| Android dependency resolution and compilation | Not run |
| Android lint | Not run |
| Phone or Desktop Head Unit | Not run |
| Real OpenFreeMap rendering | Not run |
| Authorized live OpenSky request | Not run |
| GitHub Actions workflow | Added, not run |

The core was compiled with the available Kotlin 1.9 compiler targeting JVM 17 and executed on Java 21. The bridge tests ran on Python 3.13. The pinned Android build instead uses Kotlin 2.3.0 and JDK 17, so the standalone run does not validate that full toolchain.

Kotlin parsing checks grammar only. It cannot confirm MapLibre/AndroidX symbols, Gradle DSL resolution, manifest merging, native libraries, or runtime behavior. There was no usable Android SDK or Gradle dependency cache in the validation environment, and dependency downloads were unavailable. No APK was built.

The Python tests use mocked upstream responses and real local-loopback HTTP tests. No live credentials or provider records were used.

Saved command output is in [core.txt](validation/core.txt), [bridge.txt](validation/bridge.txt), and [static.txt](validation/static.txt). Re-run the commands in [testing.md](testing.md) after changes. The Android milestone stays open until the actual build and head-unit checks pass.
