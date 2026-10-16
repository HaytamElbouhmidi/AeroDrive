# Gradle workflow correction validation

**Owner:** Haytam Elbouhmidi, Mechanical Engineer  
**Baseline:** 0.2.1-dev source archive  
**Scope:** Workflow and documentation patch. Application code is unchanged.

## Results from this check

| Check | Result | Evidence |
| --- | --- | --- |
| Standalone Kotlin core checks | 61 passed | [Core log](core.log) |
| Python bridge tests | 29 passed | [Bridge log](bridge.log) |
| Python tooling tests | 45 passed, including 33 runner tests and 12 workflow configuration checks | [Tooling log](tooling.log) |
| Source-package checks | 9 passed | [Source log](source.log) |
| Workflow YAML | Two workflows parsed | [Syntax log](workflow-syntax.log) |
| Embedded Bash steps | Seven steps parsed | [Syntax log](workflow-syntax.log) |
| Android preflight | Blocked with exit code 2 | [Preflight log](preflight.log) and [report](preflight.json) |
| Android compilation and lint | Not run | No APK produced |
| GitHub workflow execution and dependency submission | Not run | No successful remote result recorded |
| Phone, DHU, and platform feedback | Not run or submitted | Existing gates remain open |

The preflight found no Android SDK, JDK 17, or generated Gradle wrapper in the execution environment. The standalone core test used the installed Kotlin CLI and JDK, not Android Gradle Plugin. Python runner tests use mocked commands. Workflow configuration checks inspect files and version pins, they do not execute a GitHub runner.

[Environment versions](environment.log) and [machine-readable summary](summary.json) identify the exact checks. Current GitHub files were not retrieved. This patch was checked against the uploaded source archive and the reported `submit-gradle` log. No repository push or settings change was performed.

## Next evidence

Commit the correction, replace GitHub's automatic scan with the repository-owned workflow, and inspect new runs on the corrected commit. **Submit Gradle dependencies** covers the dependency graph. **Build and validate** must return its own result before phone or DHU testing.

[Correction and repository setting](../../gradle-dependency-fix.md)
