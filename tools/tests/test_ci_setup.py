"""Keep SDK setup explicit and development credentials out of the repository."""
from __future__ import annotations

import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WORKFLOWS = ("ci.yml", "dependency-submission.yml")


class CISetupTests(unittest.TestCase):
    def test_required_dotfiles_are_present(self) -> None:
        for name in (".gitignore", ".gitattributes", "bridge/.env.example", "validation/.gitignore"):
            with self.subTest(path=name):
                self.assertTrue((ROOT / name).is_file(), f"Missing source file: {name}")

    def test_example_keeps_live_access_disabled(self) -> None:
        values = {}
        for line in (ROOT / "bridge/.env.example").read_text().splitlines():
            line = line.strip()
            if line and not line.startswith("#"):
                key, value = line.split("=", 1)
                values[key.strip()] = value.strip()
        self.assertEqual(values["OPENSKY_PERMISSION_CONFIRMED"], "false")
        self.assertEqual(values["OPENSKY_CLIENT_ID"], "")
        self.assertEqual(values["OPENSKY_CLIENT_SECRET"], "")

    def test_private_config_is_ignored_but_example_is_tracked(self) -> None:
        lines = (ROOT / ".gitignore").read_text().splitlines()
        for entry in (".env", ".env.*", "!.env.example", "local.properties", "developer.properties"):
            self.assertIn(entry, lines)

    def test_sdk_setup_does_not_request_legacy_tools(self) -> None:
        for name in WORKFLOWS:
            with self.subTest(workflow=name):
                text = (ROOT / ".github/workflows" / name).read_text()
                steps = re.split(r"(?m)^      - ", text)
                setup = [step for step in steps if "uses: android-actions/setup-android@v3" in step]
                self.assertEqual(len(setup), 1)
                self.assertIn("        with:\n          packages: platform-tools\n", setup[0])
                self.assertNotRegex(setup[0], r"packages:[^\n]*\btools\s+platform-tools")

    def test_command_line_tools_version_is_pinned(self) -> None:
        for name in WORKFLOWS:
            with self.subTest(workflow=name):
                text = (ROOT / ".github/workflows" / name).read_text()
                self.assertIn("          cmdline-tools-version: '12266719'", text)

    def test_local_validation_output_is_ignored(self) -> None:
        lines = (ROOT / "validation/.gitignore").read_text().splitlines()
        self.assertIn("runs/", lines)
        self.assertIn("ci-setup/", lines)


if __name__ == "__main__":
    unittest.main()
