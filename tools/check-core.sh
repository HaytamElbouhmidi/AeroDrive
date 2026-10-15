#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
command -v kotlinc >/dev/null || { echo "Install Kotlin CLI 1.9+ or run the Gradle core tests." >&2; exit 1; }
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
kotlinc "$ROOT/core/src/main/kotlin" "$ROOT/core/src/check/kotlin" -jvm-target 17 -include-runtime -d "$WORK/checks.jar"
java -jar "$WORK/checks.jar"
