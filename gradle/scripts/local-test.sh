#!/usr/bin/env bash
set -euo pipefail

# Always run from repo root
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

# Optional: align CI-ish behavior
export CI=true
export GRADLE_OPTS="${GRADLE_OPTS:-} -Dorg.gradle.daemon=false"

# Choose tasks that match your GitHub workflow
./gradlew --no-daemon --stacktrace \
  clean \
  :app:assembleDebug \
  :app:testDebugUnitTest \
  :app:lint
