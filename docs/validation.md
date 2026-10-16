# Validation record

**Revision:** 0.2.1-dev  
**Review date:** October 15, 2026, America/Chicago  
**Source:** The supplied MapLibre/OpenSky project archive, with the validation add-on merged and the changes in this revision.

## Results actually obtained

| Check | Result | Evidence |
| --- | --- | --- |
| Kotlin core | 61 cases passed | [Core log](validation/source-review/core.log) |
| Python bridge | 29 tests passed | [Bridge log](validation/source-review/bridge.log) |
| Validation runner | 33 tests passed | [Runner log](validation/source-review/runner.log) |
| Source-package checks | 9 checks passed | [Static log](validation/source-review/static.log) |
| Kotlin/Gradle-script syntax | 20 files parsed with Kotlin PSI 1.9.0, syntax only | [Parser log](validation/source-review/kotlin-syntax.log) |
| Android build preflight | Blocked before Gradle | [Console](validation/source-review/build-attempt.log) and [run record](validation/source-review/build-attempt.json) |
| Gradle bootstrap | Failed with DNS resolution error | [Bootstrap log](validation/source-review/bootstrap.log) |
| Android compilation and lint | Not run | No claim of a clean Android build |
| APK signature or installation | Not run | No APK exists from this run |
| Phone, DHU, or physical head unit | Not run | Manual checks remain pending |
| Live OpenSky | Not run | Fixture tests do not use live data |
| Google platform feedback | Not submitted | [Review draft](review/aircraft-overlay-question.md) |

The local compiler was Kotlin CLI 1.9.0 with OpenJDK 21.0.12.1, producing JVM 17 bytecode for the standalone core checks. That run does not validate Android classes, native rendering, Kotlin Gradle plugin 2.3.0, AGP, or manifest merging.

The Python runner tests deliberately mock builds and APK metadata. Their pass count is not evidence of a built Android app. Test-fixture output within that log is not an actual project result.

## Build boundary

The uploaded source was accessible and inspected. The earlier source-access blocker is resolved.

This execution environment has no Android SDK, no JDK 17, and no downloaded Gradle wrapper. The bootstrap attempted the pinned Gradle download and exited with status 6 because `services.gradle.org` could not be resolved. A separate SDK download check could not resolve `dl.google.com`. The validation preflight exited with status 2 before invoking Gradle. No Android compiler diagnostics were produced.

The supplied source's `kotlinOptions` configuration conflicts with the documented Kotlin 2.3 deprecation errors. It has been replaced with `compilerOptions` and `JvmTarget.JVM_17`. This is a source correction based on the upstream compatibility guide, not a claim that the complete Android build passed. [Kotlin compatibility guide][1].

## Source corrections and coverage

| Change | Coverage obtained here | Remaining test |
| --- | --- | --- |
| Typed Kotlin compiler options and JDK 17 toolchain | Source inspection and wiring check | Actual Gradle configuration and compilation |
| Received Surface-handle cleanup | Source review against the callback contract | DHU reconnect and resource profiling |
| Empty visible/stable area handling | JVM regression cases | Actual host safe-area callbacks and attribution |
| Negative or unknown zoom focal point | JVM regression cases | Host pinch and rotary behavior |
| Preserved camera on activity recreation | Source wiring check | Phone rotation and process recreation |
| Proportional portrait/landscape layout | Source wiring check | Phone and large-text visual inspection |
| App palette and texture-backed car map | Resource parsing and source review | Actual map rendering and contrast |
| GitHub build workflow | YAML parsed and shell syntax checked | Workflow execution, APK build, and lint |

The [SurfaceCallback contract][2] requires releasing each Surface handle received by the app. An empty visible or stable rectangle means the area is unknown. A negative scaling focal coordinate also means the focus is unavailable. The code and manual checklist now reflect those rules.

The static guards check selected source patterns and wiring. They are not substitutes for compilation or runtime tests. Older logs in `docs/validation/` belong to the 0.2.0 source pass and are retained as historical evidence.

## Open gates

The combined build/phone/DHU roadmap item stays unchecked. The platform-feedback item also stays unchecked. Navigation, release host validation, provider agreements, privacy, and production review are still unfinished.

The next build can run through the included GitHub workflow or the local instructions in [first-build.md](first-build.md). Neither route has been executed by this source-review session.

[1]: https://kotlinlang.org/docs/compatibility-guide-23.html
[2]: https://developer.android.com/reference/androidx/car/app/SurfaceCallback
