#!/usr/bin/env bash
# Run real-app Android instrumentation against the local e2e backend.
# Requires a booted emulator and wyniki-tenis-e2e on :18087.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-/home/suchokrates1/android-sdk}"
export ANDROID_HOME="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

BASE_URL="${E2E_ANDROID_BASE_URL:-http://10.0.2.2:18087}"
ADMIN_PASSWORD="${E2E_ADMIN_PASSWORD:-e2e-admin}"

if ! adb devices 2>/dev/null | awk 'NR>1 && $2=="device" {found=1} END {exit !found}'; then
  echo "No adb device in 'device' state. Start scripts/start_dell_emulator.sh first." >&2
  exit 1
fi

dismiss_system_dialogs() {
  local i focus
  for i in 1 2 3 4 5 6; do
    focus="$(adb shell dumpsys window 2>/dev/null | grep mCurrentFocus || true)"
    if echo "$focus" | grep -Eqi 'Not Responding|Application Error|aerr_'; then
      echo "Dismissing system dialog: $focus"
      adb shell input tap 540 1342 || true
      adb shell input keyevent 66 || true
      sleep 1
    else
      return 0
    fi
  done
}

adb shell settings put global window_animation_scale 0 || true
adb shell settings put global transition_animation_scale 0 || true
adb shell settings put global animator_duration_scale 0 || true
dismiss_system_dialogs

cd "$ROOT"

echo "==> Application cold-start smoke"
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=pl.vestmedia.tennisreferee.startup.ApplicationStartupSmokeTest

echo "==> E2E package (real activities + :18087)"
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=pl.vestmedia.tennisreferee.e2e \
  -Pandroid.testInstrumentationRunnerArguments.e2e.baseUrl="$BASE_URL" \
  -Pandroid.testInstrumentationRunnerArguments.e2e.adminPassword="$ADMIN_PASSWORD"
