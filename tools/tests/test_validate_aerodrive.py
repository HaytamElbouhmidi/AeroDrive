"""Checks for the validation runner, not the Android application."""
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import zipfile

SPEC = importlib.util.spec_from_file_location("validation", Path(__file__).parents[1] / "validate_aerodrive.py")
v = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(v)


class RunnerTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)

    def apk(self, package="com.haytam.aerodrive.debug", variant="debug", filters=None,
            output="app-debug.apk", manifest=True):
        folder = self.root / "app/build/outputs/apk/debug"
        folder.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(folder / "app-debug.apk", "w") as archive:
            archive.writestr("AndroidManifest.xml" if manifest else "not-a-manifest.txt", "fixture")
        metadata = {"applicationId": package, "variantName": variant,
                    "elements": [{"outputFile": output, "filters": filters or [], "versionCode": 1}]}
        (folder / "output-metadata.json").write_text(json.dumps(metadata))
        return folder / "app-debug.apk"

    def test_java_17(self):
        self.assertEqual(v.java_major('openjdk version "17.0.16" 2025-07-15'), 17)

    def test_java_21(self):
        self.assertEqual(v.java_major('openjdk version "21.0.5"'), 21)

    def test_java_8(self):
        self.assertEqual(v.java_major('java version "1.8.0_471"'), 8)

    def test_unrecognized_java(self):
        self.assertIsNone(v.java_major("not Java"))

    def test_single_device(self):
        self.assertEqual(v.select_device("List of devices attached\nphone1\tdevice product:x\n"), "phone1")

    def test_no_device(self):
        with self.assertRaises(v.ValidationError):
            v.select_device("List of devices attached\n")

    def test_unauthorized_device(self):
        with self.assertRaises(v.ValidationError):
            v.select_device("phone1\tunauthorized\n")

    def test_offline_device(self):
        with self.assertRaises(v.ValidationError):
            v.select_device("phone1\toffline\n")

    def test_multiple_devices_need_serial(self):
        with self.assertRaises(v.ValidationError):
            v.select_device("one\tdevice\ntwo\tdevice\n")

    def test_explicit_serial(self):
        self.assertEqual(v.select_device("one\tdevice\ntwo\tdevice\n", "two"), "two")

    def test_invalid_serial(self):
        with self.assertRaises(v.ValidationError):
            v.select_device("one\tdevice\n", "missing")

    def test_explicit_sdk_wins(self):
        with patch.dict(os.environ, {"ANDROID_HOME": "/other-sdk"}):
            self.assertEqual(v.find_sdk(self.root, str(self.root)), self.root)

    def test_local_properties_sdk(self):
        (self.root / "local.properties").write_text("sdk.dir = sdk\\ folder\n")
        with patch.dict(os.environ, {}, clear=True):
            self.assertEqual(v.find_sdk(self.root, None), self.root / "sdk folder")

    def test_missing_sdk(self):
        with patch.dict(os.environ, {}, clear=True):
            self.assertIsNone(v.find_sdk(self.root, None))

    def test_artifact_metadata(self):
        apk = self.apk()
        found, data = v.debug_artifact(self.root)
        self.assertEqual(found, apk)
        self.assertEqual(data["application_id"], "com.haytam.aerodrive.debug")
        self.assertEqual(data["sha256"], v.sha256(apk))

    def test_missing_artifact(self):
        with self.assertRaises(v.ValidationError):
            v.debug_artifact(self.root)

    def test_reject_release_metadata(self):
        self.apk(variant="release")
        with self.assertRaises(v.ValidationError):
            v.debug_artifact(self.root)

    def test_reject_bad_package(self):
        self.apk(package="package && command")
        with self.assertRaises(v.ValidationError):
            v.debug_artifact(self.root)

    def test_reject_split_apk(self):
        self.apk(filters=[{"filterType": "ABI", "value": "arm64-v8a"}])
        with self.assertRaises(v.ValidationError):
            v.debug_artifact(self.root)

    def test_reject_path_escape(self):
        self.apk(output="../../elsewhere.apk")
        with self.assertRaises(v.ValidationError):
            v.debug_artifact(self.root)

    def test_reject_missing_manifest(self):
        self.apk(manifest=False)
        with self.assertRaises(v.ValidationError):
            v.debug_artifact(self.root)

    def test_command_failure_is_not_success(self):
        with patch.object(v.subprocess, "run", return_value=subprocess.CompletedProcess([], 1)):
            with self.assertRaises(v.ValidationError):
                v.run_logged(["fake"], self.root, self.root, "failure")
        self.assertTrue((self.root / "failure.log").is_file())

    def test_timeout_is_not_success(self):
        with patch.object(v.subprocess, "run", side_effect=subprocess.TimeoutExpired("fake", 1)):
            with self.assertRaises(v.ValidationError):
                v.run_logged(["fake"], self.root, self.root, "timeout", timeout=1)

    def test_cannot_start_command(self):
        with patch.object(v.subprocess, "run", side_effect=FileNotFoundError("fake")):
            with self.assertRaises(v.ValidationError):
                v.run_logged(["fake"], self.root, self.root, "missing")

    def test_fingerprint_ignores_build_outputs(self):
        source = self.root / "app/src/main/Test.kt"
        source.parent.mkdir(parents=True)
        source.write_text("class Test")
        first = v.source_fingerprint(self.root)["sha256"]
        generated = self.root / "app/build/generated.kt"
        generated.parent.mkdir()
        generated.write_text("generated fixture")
        self.assertEqual(first, v.source_fingerprint(self.root)["sha256"])

    def test_fingerprint_changes_with_source(self):
        source = self.root / "core/Test.kt"
        source.parent.mkdir()
        source.write_text("class One")
        first = v.source_fingerprint(self.root)["sha256"]
        source.write_text("class Two")
        self.assertNotEqual(first, v.source_fingerprint(self.root)["sha256"])

    def test_preflight_fails_without_source(self):
        blockers = v.preflight(self.root, None, None)
        self.assertTrue(any("settings.gradle" in item for item in blockers))
        self.assertTrue(any("JDK 17" in item for item in blockers))
        self.assertTrue(any("SDK path" in item for item in blockers))

    def test_blocked_run_keeps_manual_gates_open(self):
        out = self.root / "evidence"
        with patch.object(v, "preflight", return_value=["Source unavailable"]):
            result = v.main(["--project", str(self.root), "--out", str(out), "--check-only"])
        data = json.loads((out / "run.json").read_text())
        self.assertEqual(result, 2)
        self.assertEqual(data["build"], "not_run")
        self.assertEqual(data["phone_visual_checks"], "not_run")
        self.assertEqual(data["dhu_visual_checks"], "not_run")
        self.assertEqual(data["platform_feedback"], "not_submitted")
        self.assertFalse(data["release_ready"])

    def test_evidence_directory_is_not_overwritten(self):
        result = v.main(["--project", str(self.root), "--out", str(self.root), "--check-only"])
        self.assertEqual(result, 2)

    def test_mock_build_keeps_manual_gates_open(self):
        self.apk()
        out = self.root / "evidence"
        with patch.object(v, "preflight", return_value=[]), \
             patch.object(v, "find_java", return_value="java"), \
             patch.object(v, "find_sdk", return_value=self.root / "sdk"), \
             patch.object(v, "run_logged", return_value="") as runner:
            result = v.main(["--project", str(self.root), "--out", str(out)])
        data = json.loads((out / "run.json").read_text())
        self.assertEqual(result, 0)
        self.assertEqual(data["build"], "passed")
        self.assertEqual(data["install"], "not_run")
        self.assertEqual(data["dhu_visual_checks"], "not_run")
        self.assertFalse(data["release_ready"])
        commands = [call.args[0] for call in runner.call_args_list]
        self.assertTrue(any(":app:assembleDebug" in command and ":app:lintDebug" in command
                            and ":core:test" in command and ":app:clean" in command for command in commands))
        self.assertTrue(any("verify" in command for command in commands))

    def test_failed_build_does_not_report_old_apk(self):
        self.apk()
        out = self.root / "evidence"
        def run(command, cwd, directory, label, **kwargs):
            if label == "android-build":
                raise v.ValidationError("fixture failure")
            return ""
        with patch.object(v, "preflight", return_value=[]), \
             patch.object(v, "find_java", return_value="java"), \
             patch.object(v, "find_sdk", return_value=self.root / "sdk"), \
             patch.object(v, "run_logged", side_effect=run):
            result = v.main(["--project", str(self.root), "--out", str(out)])
        data = json.loads((out / "run.json").read_text())
        self.assertEqual(result, 2)
        self.assertEqual(data["build"], "failed")
        self.assertIsNone(data["apk"])

    def test_dhu_tunnel_uses_forward_not_reverse(self):
        with patch.object(v, "run_logged") as runner:
            v.prepare_tunnel(["adb", "-s", "phone"], self.root, self.root, reverse=False)
        self.assertEqual(runner.call_args.args[0][-4:], ["forward", "--no-rebind", "tcp:5277", "tcp:5277"])

    def test_bridge_tunnel_uses_reverse_not_forward(self):
        with patch.object(v, "run_logged") as runner:
            v.prepare_tunnel(["adb", "-s", "phone"], self.root, self.root, reverse=True)
        self.assertEqual(runner.call_args.args[0][-4:], ["reverse", "--no-rebind", "tcp:8765", "tcp:8765"])


if __name__ == "__main__":
    unittest.main()
