# First build and phone test

## Dependency-scan setup

Use the repository's **Submit Gradle dependencies** workflow for the dependency graph and **Build and validate** for the APK. Disable only GitHub's **Automatic dependency submission** setting after adding the replacement workflow. Keep the dependency graph and Dependabot alerts enabled. The old automatic job does not run the project's wrapper bootstrap and can select an incompatible Gradle version.

See [the Gradle workflow correction](gradle-dependency-fix.md) for the exact setting, first run, and wrapper-commit instructions.

**Revision:** 0.2.1-dev  
**Owner:** Haytam Elbouhmidi, Mechanical Engineer  
**Current gate:** The Android build and code checks passed in the [October 16, 2026 run](https://github.com/HaytamElbouhmidi/AeroDrive/actions/runs/38916734207) at commit `c5ae04c`. A test APK was uploaded. The next step is a recorded phone test, followed by the separate car-display checklist.

## Build using GitHub Actions

This path does not require an Android SDK installation on the computer used to open GitHub.

To use the already verified build, open the [successful run](https://github.com/HaytamElbouhmidi/AeroDrive/actions/runs/38916734207), find **Artifacts**, and download `aerodrive-debug-3`. Extract the ZIP, then continue to [the first phone test](#first-phone-test-only-after-the-build-passes). You may need to sign in to GitHub. If the artifact has expired, or you need to test a newer revision, use the steps below to create a new build.

1. Extract the project archive. Put the contents of its `aerodrive` folder at the repository root. The root should contain `app`, `core`, `bridge`, `.github`, `build.gradle.kts`, and `README.md`. Do not commit only the ZIP or add an extra `aerodrive` nesting level. Preserve any private local configuration outside Git.
2. Commit and push. Open **Actions > Build and validate**. A push starts a run. To start one manually, the workflow must exist on the default branch and the account must have write access. Select **Run workflow**. [GitHub workflow instructions][1].
3. Inspect the run. Both **Source and Python tests** and **Debug APK and lint** must pass. Both passed in the linked October 16 run. Check every new run separately before using its APK.
4. In a successful run's **Artifacts** section, download `aerodrive-debug-<run number>` and extract it. It contains the APK, the signature result, and `run.json`. Retain `aerodrive-build-evidence-<run number>` as well. Downloading artifacts requires an appropriate signed-in GitHub account. [Artifact instructions][2].

If a run fails, keep its first failing step and the evidence artifact when one exists. SDK setup can fail before an artifact is produced, so the failed step log is also useful. Do not use an older APK to claim the new revision passed.

## Local alternative

The complete setup is in [setup.md](setup.md). I use JDK 17, SDK Platform 36, Build Tools 35.0.0, Python 3.10+, and the included Gradle bootstrap script. The [AGP compatibility table][3] documents the Gradle and JDK requirements.

After bootstrap and SDK setup:

```powershell
py -3 tools/validate_aerodrive.py --check-only
py -3 tools/validate_aerodrive.py
```

For Linux/macOS, replace `py -3` with `python3`. Review the new run directory under `validation/runs/`. A failed preflight is not an Android compiler failure. A successful build still leaves the manual checks pending.

## First phone test, only after the build passes

I start with a phone running Android 9 or later and the built-in simulation. No bridge, OpenSky account, GPS permission, or car is needed. I perform the test while stationary.

The debug package is `com.haytam.aerodrive.debug`. I install the APK from the successful run with Android Studio or ADB:

```powershell
adb install -r -t app-debug.apk
```

This command assumes `adb` is on PATH and the terminal is in the folder containing the extracted APK. Alternatively, a local build can use:

```powershell
py -3 tools/validate_aerodrive.py --install
```

Different local or CI machines may generate different debug signing keys. A signature mismatch can therefore block an update. I do not silently uninstall an existing app or delete its data. I retain the installation error and resolve the signing mismatch explicitly.

| Step | Expected observation |
| --- | --- |
| Launch | The title includes `0.2.1-dev`. The app does not crash or request location access. |
| Street map | Austin and readable map attribution appear with networking available. Aircraft are fictional and limited to five. If the hosted map fails, the fallback is explicitly labeled as a test layout. |
| Test map | Tapping **Test map** loads the bundled schematic. It continues working with networking disabled. |
| Aircraft controls | **Nearest** displays simulated details. **Hide aircraft** removes symbols. **Show aircraft** restores them. **Pause** stops simulated movement. |
| Freshness | **Stale test** removes old positions. **Restore data** restores the simulation. |
| Rotation | Portrait and landscape keep a usable map and reachable controls. The camera does not reset just because the activity is recreated. |

I record the phone model, Android version, APK hash, and one screenshot in each orientation. Any failure needs the button or action that caused it and the visible error. These observations cover the initial smoke test, not the entire [phone/DHU checklist](testing.md).

## Stop before the car test

I complete the initial phone test before setting up the DHU. No physical driving is needed. DHU observations and platform feedback remain separate requirements, and neither is completed by a successful APK build.

[1]: https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow
[2]: https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts
[3]: https://developer.android.com/build/releases/agp-8-13-0-release-notes
