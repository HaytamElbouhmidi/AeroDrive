"""Keep the dependency scan aligned with the Android build toolchain."""
from __future__ import annotations

import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


class WorkflowConfigTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.scan = (ROOT / ".github/workflows/dependency-submission.yml").read_text()
        cls.ci = (ROOT / ".github/workflows/ci.yml").read_text()
        cls.app = (ROOT / "app/build.gradle.kts").read_text()
        cls.core = (ROOT / "core/build.gradle.kts").read_text()

    def test_submission_selects_gradle_explicitly(self) -> None:
        self.assertIn("uses: gradle/actions/dependency-submission@v4", self.scan)
        self.assertRegex(self.scan, r"(?m)^          gradle-version: '8\.13'$")
        self.assertNotIn("./gradlew", self.scan)

    def test_gradle_matches_both_bootstrap_scripts(self) -> None:
        shell = (ROOT / "tools/bootstrap-gradle.sh").read_text()
        powershell = (ROOT / "tools/bootstrap-gradle.ps1").read_text()
        self.assertIn("VERSION=8.13", shell)
        self.assertIn('$Version = "8.13"', powershell)
        self.assertIn("gradle-version: '8.13'", self.scan)

    def test_java_matches_the_compilation_toolchains(self) -> None:
        self.assertIn("jvmToolchain(17)", self.app)
        self.assertIn("jvmToolchain(17)", self.core)
        for workflow in (self.scan, self.ci):
            self.assertIn("java-version: '17'", workflow)

    def test_sdk_components_match_the_app(self) -> None:
        self.assertIn("compileSdk = 36", self.app)
        self.assertIn('buildToolsVersion = "35.0.0"', self.app)
        sdk = 'sdkmanager "platforms;android-36" "build-tools;35.0.0" "platform-tools"'
        for workflow in (self.scan, self.ci):
            self.assertIn(sdk, workflow)
            self.assertIn("android-actions/setup-android@v3", workflow)

    def test_runner_images_do_not_float(self) -> None:
        for workflow in (self.scan, self.ci):
            runners = re.findall(r"runs-on: ([^\n]+)", workflow)
            self.assertTrue(runners)
            self.assertTrue(all(runner == "ubuntu-24.04" for runner in runners))

    def test_submission_is_main_branch_only(self) -> None:
        self.assertIn("    branches: [main]", self.scan)
        self.assertIn("    if: github.ref == 'refs/heads/main'", self.scan)
        self.assertIn("  workflow_dispatch:", self.scan)
        self.assertNotIn("pull_request", self.scan)

    def test_write_permission_is_scoped_to_submission(self) -> None:
        self.assertIn("permissions:\n  contents: read\n", self.scan)
        self.assertIn("    permissions:\n      contents: write\n", self.scan)
        self.assertIn("persist-credentials: false", self.scan)
        self.assertNotIn("contents: write", self.ci)

    def test_submission_uses_the_repository_root(self) -> None:
        self.assertIn("build-root-directory: '.'", self.scan)
        self.assertTrue((ROOT / "settings.gradle.kts").is_file())
        self.assertTrue((ROOT / "build.gradle.kts").is_file())

    def test_submission_keeps_errors_visible(self) -> None:
        self.assertIn("--no-configuration-cache --stacktrace", self.scan)
        self.assertNotIn("continue-on-error", self.scan)
        self.assertIn("timeout-minutes: 20", self.scan)

    def test_wrapper_is_created_before_it_is_uploaded(self) -> None:
        create = self.ci.index("bash tools/bootstrap-gradle.sh")
        upload = self.ci.index("name: aerodrive-gradle-wrapper-")
        build = self.ci.index("run: python tools/validate_aerodrive.py")
        self.assertLess(create, upload)
        self.assertLess(upload, build)

    def test_wrapper_artifact_contains_all_four_files(self) -> None:
        block = self.ci.split("      - name: Upload the Gradle wrapper\n", 1)[1]
        block = block.split("      - name:", 1)[0]
        for name in ("gradlew", "gradlew.bat", "gradle/wrapper/gradle-wrapper.jar",
                     "gradle/wrapper/gradle-wrapper.properties"):
            self.assertIn("            " + name + "\n", block)
        self.assertIn("if-no-files-found: error", block)

    def test_wrapper_line_endings_are_declared(self) -> None:
        attributes = (ROOT / ".gitattributes").read_text()
        self.assertIn("gradlew text eol=lf", attributes)
        self.assertIn("*.jar binary", attributes)


if __name__ == "__main__":
    unittest.main()
