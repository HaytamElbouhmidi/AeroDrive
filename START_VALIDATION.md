# Build and review validation

This is an add-on for my existing AeroDrive MapLibre/OpenSky project, not a replacement source archive and not an APK. It contains validation tooling and updated documentation. It does not contain the app, core, bridge, or Gradle wrapper from the original project.

I merge these files into the root of the extracted `AeroDrive-MapLibre-OpenSky` project. I keep the existing source, Gradle files, `LICENSE`, and third-party notices. I compare the updated `README.md`, `docs/testing.md`, and `docs/validation.md` before replacing local versions that have newer edits.

## Run on Windows

I install JDK 17, Android SDK Platform 36, Build Tools 35.0.0, Platform-Tools, and the Android Auto Desktop Head Unit Emulator through Android Studio. Python 3.10 or newer runs the validation tools. I accept SDK licenses through the normal setup process.

The original source handoff omitted the Gradle wrapper binaries. In the existing project, I run its bootstrap script first when `gradle/wrapper/gradle-wrapper.jar` is missing:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\bootstrap-gradle.ps1
```

Then I run:

```powershell
py -3 tools/validate_aerodrive.py --check-only
py -3 tools/validate_aerodrive.py
```

If SDK discovery needs an explicit path:

```powershell
py -3 tools/validate_aerodrive.py --sdk "$env:LOCALAPPDATA\Android\Sdk"
```

For an authorized USB-connected phone, with the Android Auto head-unit server already running:

```powershell
py -3 tools/validate_aerodrive.py --install --dhu
```

I add `--serial SERIAL` when more than one device is connected. I add `--bridge-tunnel` only for the local fixture bridge. The command rebuilds the app before installation. It does not start a live OpenSky connection.

## Linux and macOS

The same runner works with `python3` in place of `py -3`. I use the existing `bash tools/bootstrap-gradle.sh` when the wrapper needs bootstrapping. The runner invokes the official wrapper JAR through Java, so it does not depend on shell-script executable bits or Windows batch quoting.

## Evidence and limits

Each run creates a new directory in `validation/runs/`. A successful build records logs, source-input hashes, the debug APK, its SHA-256, signature verification, and a fresh manual checklist. Existing evidence directories are never overwritten.

The runner calls the existing bridge tests and static checker, then runs a clean `:core:test`, `:app:assembleDebug`, and `:app:lintDebug`. It stops on failure and does not substitute an older APK.

`--install` only verifies installation. It does not claim the phone UI works. `--dhu` prepares the connection and records the DHU version. It does not claim the map rendered. I finish the observable checks in [docs/testing.md](docs/testing.md) and record the results against the APK hash.

Logs can include device identifiers and local paths. The `runs/` directory stays out of version control. I inspect and redact logs and captures before sharing them.

The [platform review request](docs/review/aircraft-overlay-question.md) is a draft. It has not been submitted. [Review routing](docs/platform-review.md) separates a support question, testing-track feedback, and release approval.

## Current status

The new runner's tests pass. Android source was not accessible for this validation session, so no Android build, installation, phone test, DHU test, or Google submission was completed. The two roadmap items remain open. See [validation results](docs/validation.md).
