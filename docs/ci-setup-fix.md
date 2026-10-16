# CI setup correction

I checked the two repository workflows on October 16, 2026, for commit `1e9d8561f11d724bdc0447bd2ea0ab627e6f7945`.

## Observed failures

[Build and validate, run 2](https://github.com/HaytamElbouhmidi/AeroDrive/actions/runs/38914183487) reached the source check. It stopped with a missing `bridge/.env.example` error. The Android job was skipped. The repository tree also lacked `.gitignore` and `.gitattributes`, which later checks require, plus `validation/.gitignore`.

[Submit Gradle dependencies, run 2](https://github.com/HaytamElbouhmidi/AeroDrive/actions/runs/38914183616) selected JDK 17, then failed while `setup-android@v3` called `sdkmanager tools`. The log reported `Failed to find package 'tools'`. The Gradle dependency step never ran.

Neither run produced an APK. These failures do not establish whether the Android source compiles.

## Correction

I restored the four missing source-controlled dotfiles and kept the OpenSky credential fields empty. Nothing in this patch enables live data or adds private credentials.

I set `packages: platform-tools` on the Android setup step in both workflows, retained command-line tools build `12266719`, and disabled verbose SDK license text in the logs. The action still handles command-line tools and SDK licenses. The existing next step installs Platform 36 and Build Tools 35.0.0. I did not change Gradle, AGP, Kotlin, or the Android application code.

The setup action documents the [package override and removed tools package](https://github.com/android-actions/setup-android#additional-packages). Android documents [sdkmanager package installation](https://developer.android.com/tools/sdkmanager).

## Apply the patch

Merge these paths into the repository root and commit them together:

```text
.gitattributes
.gitignore
.github/workflows/ci.yml
.github/workflows/dependency-submission.yml
bridge/.env.example
validation/.gitignore
tools/tests/test_ci_setup.py
README.md
docs/ci-setup-fix.md
```

The ZIP is a patch, not a complete source project. Keep the existing files that are not listed. Do not upload the ZIP itself or put the extracted patch inside another repository folder. Include the dot-prefixed files explicitly. Do not create or upload a real `.env` file.

Before committing through the GitHub website, check that the pending file list contains all nine paths. Commit to `main` with `Fix SDK setup and restore development config templates`. Both workflows have push triggers. Inspect the new runs for the new commit, not a rerun of the old failed commit.

No additional repository security-setting change is part of this patch. Leave the custom dependency workflow in place. Do not weaken the source checks to hide missing files.

## Validation boundary

The local validation uses the previously supplied 0.2.1 source archive with the earlier Gradle patch and this correction applied. The baseline README, both workflow files, and app build file were checked against the blob hashes returned for the failing GitHub commit. A fresh full repository archive could not be downloaded because outbound DNS resolution was unavailable.

The source-package checker, Python bridge tests, Python tooling tests, and YAML/Bash syntax checks are run locally. SDK downloads, Android compilation, Android lint, dependency submission, and phone/DHU behavior are not validated by those tests. The new workflow settings still need a fresh GitHub run.

The first device test remains blocked until a debug APK and its build evidence are available.

## Local results for this patch

| Check | Result |
| --- | --- |
| Source-package checks | 9 passed |
| Bridge tests | 29 passed |
| Tooling tests | 51 passed |
| YAML parsing | Both workflows passed |
| Embedded Bash syntax | Passed |
| Real SDK setup on GitHub | Not rerun with this patch |
| Android compilation and lint | Not run |
| Phone/DHU testing | Not run |

These results are local validation of the patch, not new GitHub run results.
