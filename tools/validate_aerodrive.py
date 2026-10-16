#!/usr/bin/env python3
"""Build the debug app and keep the evidence from each validation run."""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
from typing import Any
import zipfile


class ValidationError(Exception):
    pass


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def java_major(text: str) -> int | None:
    match = re.search(r'(?:openjdk|java)\s+version\s+"(\d+)(?:\.(\d+))?', text)
    if not match:
        return None
    first = int(match.group(1))
    return int(match.group(2)) if first == 1 and match.group(2) else first


def find_java() -> str | None:
    home = os.environ.get("JAVA_HOME")
    if home:
        candidate = Path(home) / "bin" / ("java.exe" if os.name == "nt" else "java")
        return str(candidate) if candidate.is_file() else None
    return shutil.which("java")


def find_sdk(project: Path, explicit: str | None) -> Path | None:
    for value in (explicit, os.environ.get("ANDROID_HOME"), os.environ.get("ANDROID_SDK_ROOT")):
        if value:
            return Path(value).expanduser().resolve()
    properties = project / "local.properties"
    if properties.is_file():
        for line in properties.read_text(encoding="utf-8").splitlines():
            match = re.match(r"\s*sdk\.dir\s*=\s*(.*?)\s*$", line)
            if match:
                value = re.sub(r"\\([\\:= ])", r"\1", match.group(1))
                result = Path(value).expanduser()
                return (project / result).resolve() if not result.is_absolute() else result.resolve()
    return None


def select_device(text: str, requested: str | None = None) -> str:
    devices = {}
    for line in text.splitlines():
        columns = line.split()
        if len(columns) >= 2 and columns[1] in {"device", "offline", "unauthorized", "recovery"}:
            devices[columns[0]] = columns[1]
    if requested:
        if devices.get(requested) != "device":
            raise ValidationError(f"Device {requested} is not connected and authorized.")
        return requested
    if len(devices) != 1:
        raise ValidationError("Connect exactly one device, or select one with --serial.")
    serial, state = next(iter(devices.items()))
    if state != "device":
        raise ValidationError(f"Device {serial} is {state}. Authorize USB debugging on the phone.")
    return serial


def debug_artifact(project: Path) -> tuple[Path, dict[str, Any]]:
    directory = project / "app/build/outputs/apk/debug"
    metadata = directory / "output-metadata.json"
    if not metadata.is_file():
        raise ValidationError("The debug APK metadata is missing. No build result will be inferred.")
    data = json.loads(metadata.read_text(encoding="utf-8"))
    if data.get("variantName") != "debug":
        raise ValidationError("The APK metadata is not for the debug variant.")
    package = data.get("applicationId", "")
    if not re.fullmatch(r"[A-Za-z]\w*(?:\.[A-Za-z]\w*)+", package):
        raise ValidationError("The debug application ID is missing or invalid.")
    elements = data.get("elements", [])
    if len(elements) != 1 or elements[0].get("filters"):
        raise ValidationError("This runner expects one universal debug APK, not ABI or density splits.")
    name = elements[0].get("outputFile", "")
    if not isinstance(name, str) or not name:
        raise ValidationError("No APK filename was found in the build metadata.")
    apk = (directory / name).resolve()
    if directory.resolve() not in apk.parents or apk.suffix != ".apk" or not apk.is_file():
        raise ValidationError("The debug APK path is invalid or the APK is missing.")
    with zipfile.ZipFile(apk) as archive:
        if "AndroidManifest.xml" not in archive.namelist():
            raise ValidationError("The APK does not contain an Android manifest.")
    return apk, {"application_id": package, "sha256": sha256(apk),
                 "version_code": elements[0].get("versionCode"),
                 "version_name": elements[0].get("versionName")}


def run_logged(command: list[str], cwd: Path, out: Path, label: str,
               timeout: int = 900, env: dict[str, str] | None = None) -> str:
    print(f"Running {label}", flush=True)
    log = out / f"{label}.log"
    with log.open("w", encoding="utf-8") as stream:
        stream.write(json.dumps(command) + "\n\n")
        stream.flush()
        try:
            result = subprocess.run(command, cwd=cwd, stdout=stream, stderr=subprocess.STDOUT,
                                    timeout=timeout, check=False, env=env)
        except subprocess.TimeoutExpired as error:
            raise ValidationError(f"{label} timed out. See {log.name}.") from error
        except OSError as error:
            raise ValidationError(f"{label} could not start: {error}") from error
    text = log.read_text(encoding="utf-8", errors="replace")
    if result.returncode:
        raise ValidationError(f"{label} failed with exit code {result.returncode}. See {log.name}.")
    return text


def source_fingerprint(project: Path) -> dict[str, Any]:
    # Hash source inputs, not local credentials or generated build output.
    extensions = {".kt", ".kts", ".java", ".xml", ".py", ".toml", ".json", ".pro"}
    entries = {}
    for folder in ("app", "core", "bridge", "gradle"):
        base = project / folder
        if not base.is_dir():
            continue
        for path in sorted(base.rglob("*")):
            relative = path.relative_to(project)
            if any(part in {"build", ".gradle", "__pycache__", ".venv", "node_modules"}
                   for part in relative.parts):
                continue
            if path.is_file() and not path.is_symlink() and path.suffix in extensions:
                entries[str(relative).replace("\\", "/")] = sha256(path)
    for name in ("build.gradle.kts", "build.gradle", "settings.gradle.kts", "settings.gradle",
                 "gradle.properties", "gradle/wrapper/gradle-wrapper.jar",
                 "gradle/wrapper/gradle-wrapper.properties"):
        path = project / name
        if path.is_file() and not path.is_symlink():
            entries[name] = sha256(path)
    encoded = json.dumps(entries, sort_keys=True).encode("utf-8")
    return {"scope": "Selected source and Gradle inputs, not a full repository attestation",
            "sha256": hashlib.sha256(encoded).hexdigest(), "files": entries}


def preflight(project: Path, sdk: Path | None, java: str | None) -> list[str]:
    blockers = []
    for base in ("settings.gradle", "app/build.gradle", "core/build.gradle"):
        if not any((project / (base + suffix)).is_file() for suffix in ("", ".kts")):
            blockers.append(f"Missing {base} or {base}.kts")
    for name in ("app/src/main/AndroidManifest.xml", "tools/check-project.py",
                 "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties"):
        if not (project / name).is_file():
            blockers.append(f"Missing {name}")
    if not list((project / "bridge").glob("test_*.py")):
        blockers.append("No bridge/test_*.py tests were found")
    if not java:
        blockers.append("JDK 17 was not found. Set JAVA_HOME to the JDK 17 installation")
    else:
        try:
            version = subprocess.run([java, "-version"], capture_output=True, text=True,
                                     timeout=20, check=False)
            if version.returncode or java_major(version.stdout + version.stderr) != 17:
                blockers.append("Use JDK 17 for this run to match the documented prototype toolchain")
        except (OSError, subprocess.TimeoutExpired):
            blockers.append("The selected Java executable did not respond successfully")
    if sdk is None:
        blockers.append("Android SDK path is missing. Set ANDROID_HOME or pass --sdk")
    else:
        suffix = ".exe" if os.name == "nt" else ""
        for name in ("platforms/android-36/android.jar", f"build-tools/35.0.0/aapt2{suffix}",
                     "build-tools/35.0.0/lib/apksigner.jar"):
            if not (sdk / name).is_file():
                blockers.append(f"Missing SDK component: {name}")
    return blockers


def prepare_tunnel(adb: list[str], project: Path, out: Path, reverse: bool) -> None:
    direction = "reverse" if reverse else "forward"
    port = "tcp:8765" if reverse else "tcp:5277"
    # --no-rebind avoids replacing a tunnel that another session owns.
    run_logged(adb + [direction, "--no-rebind", port, port], project, out,
               f"adb-{direction}", timeout=30)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", default=".")
    parser.add_argument("--sdk")
    parser.add_argument("--out", help="A new directory for this run. Existing directories are rejected.")
    parser.add_argument("--check-only", action="store_true")
    parser.add_argument("--install", action="store_true", help="Install the verified debug APK on a phone")
    parser.add_argument("--serial", help="ADB serial when more than one device is connected")
    parser.add_argument("--dhu", action="store_true", help="Prepare the ADB head-unit tunnel, not a visual test")
    parser.add_argument("--bridge-tunnel", action="store_true")
    args = parser.parse_args(argv)
    if args.check_only and (args.install or args.dhu or args.bridge_tunnel):
        parser.error("--check-only cannot install or prepare tunnels")
    project = Path(args.project).expanduser().resolve()
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
    out = Path(args.out).expanduser().resolve() if args.out else project / "validation/runs" / stamp
    try:
        out.mkdir(parents=True, exist_ok=False)
    except OSError as error:
        print(f"Cannot create a new evidence directory: {error}", file=sys.stderr)
        return 2
    report: dict[str, Any] = {
        "schema_version": 1, "started_at": utc_now(), "status": "blocked", "blockers": [],
        "build": "not_run", "install": "not_run", "phone_visual_checks": "not_run",
        "dhu_visual_checks": "not_run", "platform_feedback": "not_submitted",
        "release_ready": False, "apk": None,
    }
    exit_code = 2
    try:
        sdk, java = find_sdk(project, args.sdk), find_java()
        report["blockers"] = preflight(project, sdk, java)
        report["source_fingerprint"] = source_fingerprint(project)
        template = Path(__file__).resolve().parent.parent / "validation/checklist-template.json"
        if template.is_file():
            shutil.copyfile(template, out / "manual-checklist.json")
        if report["blockers"]:
            for blocker in report["blockers"]:
                print(f"BLOCKED: {blocker}")
            return 2
        report["status"] = "preflight_passed"
        if args.check_only:
            exit_code = 0
            return exit_code
        assert java is not None and sdk is not None
        environment = os.environ.copy()
        environment["ANDROID_HOME"] = str(sdk)
        environment["ANDROID_SDK_ROOT"] = str(sdk)
        run_logged([java, "-version"], project, out, "java-version", timeout=30)
        gradle = [java, "-Xmx64m", "-Xms64m", "-Dorg.gradle.appname=gradlew", "-classpath",
                  str(project / "gradle/wrapper/gradle-wrapper.jar"),
                  "org.gradle.wrapper.GradleWrapperMain"]
        run_logged(gradle + ["--version"], project, out, "gradle-version", env=environment)
        run_logged([sys.executable, "-m", "unittest", "discover", "-s", "bridge", "-p", "test_*.py", "-v"],
                   project, out, "bridge-tests")
        run_logged([sys.executable, "tools/check-project.py"], project, out, "project-checks")
        report["build"] = "running"
        run_logged(gradle + ["--no-daemon", "--console=plain", "--stacktrace", ":core:clean", ":app:clean",
                             ":core:test", ":app:assembleDebug", ":app:lintDebug"],
                   project, out, "android-build", env=environment)
        apk, info = debug_artifact(project)
        run_logged([java, "-jar", str(sdk / "build-tools/35.0.0/lib/apksigner.jar"),
                    "verify", "--verbose", str(apk)], project, out, "apk-signature", timeout=60)
        shutil.copyfile(apk, out / apk.name)
        info["file"] = apk.name
        report["apk"], report["build"] = info, "passed"
        report["status"] = "build_passed_manual_checks_pending"
        if args.install or args.dhu or args.bridge_tunnel:
            adb_path = sdk / "platform-tools" / ("adb.exe" if os.name == "nt" else "adb")
            if not adb_path.is_file():
                raise ValidationError("Install Android SDK Platform-Tools before phone or DHU setup.")
            listing = run_logged([str(adb_path), "devices", "-l"], project, out, "adb-devices", timeout=30)
            serial = select_device(listing, args.serial)
            adb = [str(adb_path), "-s", serial]
            report["device_serial"] = serial
            for prop in ("ro.product.model", "ro.build.version.release", "ro.build.version.sdk"):
                run_logged(adb + ["shell", "getprop", prop], project, out, prop, timeout=30)
            run_logged(adb + ["shell", "dumpsys", "package", "com.google.android.projection.gearhead"],
                       project, out, "android-auto-package", timeout=30)
            if args.install:
                text = run_logged(adb + ["install", "-r", "-t", str(out / apk.name)],
                                  project, out, "apk-install", timeout=180)
                if not re.search(r"^Success\s*$", text, re.MULTILINE):
                    raise ValidationError("ADB did not confirm installation. See apk-install.log.")
                report["install"] = "passed"
            if args.dhu:
                dhu = sdk / "extras/google/auto" / ("desktop-head-unit.exe" if os.name == "nt" else "desktop-head-unit")
                if not dhu.is_file():
                    raise ValidationError("Install Android Auto Desktop Head Unit Emulator in SDK Manager.")
                run_logged([str(dhu), "--version"], dhu.parent, out, "dhu-version", timeout=30)
                prepare_tunnel(adb, project, out, reverse=False)
                report["dhu_tunnel"] = "prepared"
            if args.bridge_tunnel:
                prepare_tunnel(adb, project, out, reverse=True)
                report["bridge_tunnel"] = "prepared"
        exit_code = 0
        return exit_code
    except (ValidationError, OSError, ValueError, zipfile.BadZipFile) as error:
        report["blockers"].append(str(error))
        report["status"] = "failed"
        if report["build"] == "running":
            report["build"] = "failed"
        print(f"FAILED: {error}", file=sys.stderr)
        return 2
    finally:
        report["finished_at"] = utc_now()
        (out / "run.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        print(f"Evidence: {out}")
        print("Phone/DHU visual checks and platform feedback are not completed by this runner.")


if __name__ == "__main__":
    raise SystemExit(main())
