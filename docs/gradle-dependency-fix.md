# Gradle dependency-submission fix

**Owner:** Haytam Elbouhmidi, Mechanical Engineer  
**Source baseline:** 0.2.1-dev  
**Scope:** GitHub workflows and build documentation. No application code changes.

## Failure

The reported `submit-gradle` job could not find `./gradlew`, then tried the runner's Gradle 9.8.1 installation. Applying Android Gradle Plugin 8.13.2 failed because Gradle 9.6 removed the internal Problems API that AGP 8.x uses. The job reported Java 21, but that is not the failure named in this stack trace. [Gradle compatibility note][1].

The source package includes a bootstrap script, not the generated wrapper files. The APK workflow runs that script, but GitHub's automatic dependency-submission job does not. Creating the wrapper in one job does not add it to Git or make it available to another job.

The Android build file was found at `app/build.gradle.kts`. This log does not call for moving the app to a different directory.

## Toolchain

I am keeping AGP 8.13.2, Kotlin 2.3.0, Gradle 8.13, JDK 17, Android SDK Platform 36, and Build Tools 35.0.0. These match the existing source configuration and the AGP 8.13 compatibility table. I am not changing the project to Gradle 9.5 or upgrading AGP as part of this correction. [Android compatibility notes][2].

## Repository changes

| File | Change |
| --- | --- |
| `.github/workflows/dependency-submission.yml` | New dependency job with an explicit Gradle 8.13 selection, JDK 17, Android SDK setup, and Ubuntu 24.04 |
| `.github/workflows/ci.yml` | Retain the generated official wrapper as a separate artifact before the APK build |
| `.gitattributes` | Keep the Unix wrapper and shell scripts on LF line endings and treat JARs as binary |
| `tools/tests/test_workflow_config.py` | Check the workflow pins, submission scope, and wrapper artifact wiring |
| `README.md` and build notes | Separate the dependency scan from the APK build and document the repository setting |

The Gradle action supports an explicit `gradle-version`. With that input, it installs and invokes the selected Gradle version rather than requiring a checked-in wrapper. That is why the new dependency job can run with the existing source package. [Action input contract][3].

## Apply the correction

1. Merge the patch into the existing repository root and commit the changed files. Keep `.github/workflows/dependency-submission.yml` under `.github`, not in the `app` directory. Do not upload the ZIP as the source change.
2. In repository **Settings > Advanced Security > Dependency graph**, set **Automatic dependency submission** to **Disabled**. Leave **Dependency graph** and **Dependabot alerts** enabled. This replaces the unsuitable GitHub-managed scan with the repository-owned workflow, not with no dependency scanning. [GitHub setting][4].
3. Open **Actions > Submit Gradle dependencies** and inspect the run for the new commit. A push to `main` starts it, and **Run workflow** is also available. Check **Build and validate** separately for the APK result.

Adding this YAML does not automatically disable GitHub's managed scan. Without the setting change, the old `submit-gradle` job can keep failing alongside the new workflow. [Managed workflow behavior][5].

Do not use **Re-run jobs** on the old failed run to test this patch. A rerun uses the original commit. Start or inspect a run on the corrected `main` commit. [GitHub rerun behavior][6].

## Expected results

**Submit Gradle dependencies** should report the selected Gradle 8.13 toolchain and submit a dependency graph. It does not compile or publish an APK.

**Build and validate** creates the checksum-verified wrapper and retains it as `aerodrive-gradle-wrapper-<run number>`. It then attempts the core tests, debug build, lint, and APK signature check. It uploads `aerodrive-debug-<run number>` only after those checks succeed. A wrapper artifact on its own is not an APK-build pass.

The workflow pins Ubuntu 24.04. The pasted `ubuntu-latest` migration notice came from the separate automatic scan and was not the cause of its Gradle exception.

## Commit the wrapper after it is generated

The wrapper binaries are still not included in this source patch. Once **Build and validate** reaches the wrapper-upload step, download `aerodrive-gradle-wrapper-<run number>` and extract its contents into the repository root. It contains:

```text
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
```

Alternatively, generate the same files locally with `tools/bootstrap-gradle.ps1` or `tools/bootstrap-gradle.sh`. Then commit them:

```bash
git add gradlew gradlew.bat gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties
git update-index --chmod=+x gradlew
git commit -m "Add the Gradle 8.13 wrapper"
git push
```

Gradle recommends committing all wrapper files, including the JAR. The Unix launcher's executable bit matters on Linux. The new dependency workflow does not need this follow-on step to select Gradle 8.13. [Wrapper guidance][7].

## Validation boundary

Workflow configuration checks and the existing offline suites can run locally. They cannot establish that GitHub downloaded dependencies, submitted a graph, or built the Android app. No successful GitHub run or APK is claimed by this patch. The phone/DHU and platform-feedback roadmap items remain open.

See [the correction's validation record](validation/gradle-workflow-fix/README.md). The previous source-review evidence remains in [validation.md](validation.md).

[1]: https://docs.gradle.org/9.8.1/userguide/upgrading_version_9.html#agp_8x_incompatible
[2]: https://developer.android.com/build/releases/agp-8-13-0-release-notes
[3]: https://raw.githubusercontent.com/gradle/actions/v4/dependency-submission/action.yml
[4]: https://docs.github.com/en/code-security/how-tos/secure-your-supply-chain/secure-your-dependencies/submit-dependencies-automatically
[5]: https://docs.github.com/en/code-security/reference/supply-chain-security/automatic-dependency-submission
[6]: https://docs.github.com/en/actions/how-tos/manage-workflow-runs/re-run-workflows-and-jobs
[7]: https://docs.gradle.org/current/userguide/gradle_wrapper.html
