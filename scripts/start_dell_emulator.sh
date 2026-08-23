#!/usr/bin/env bash
# Start the headless dell-e2e AVD used for Android instrumentation / E2E.
set -euo pipefail

export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-/home/suchokrates1/android-sdk}"
export ANDROID_HOME="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

AVD_NAME="${AVD_NAME:-dell-e2e}"

if ! emulator -accel-check 2>/dev/null | grep -q 'is installed and usable'; then
  echo "KVM is not usable. Add this user to the kvm group and ensure /dev/kvm is rw." >&2
  emulator -accel-check || true
  exit 1
fi

if ! avdmanager list avd 2>/dev/null | grep -q "Name: ${AVD_NAME}"; then
  echo "AVD ${AVD_NAME} missing; create it first." >&2
  exit 1
fi

if adb devices 2>/dev/null | awk 'NR>1 && $2=="device" {found=1} END {exit !found}'; then
  echo "An emulator/device is already in 'device' state."
  adb devices -l
  exit 0
fi

EMU_CMD=(emulator -avd "$AVD_NAME" \
  -no-window \
  -no-audio \
  -no-boot-anim \
  -gpu swiftshader_indirect \
  -accel on \
  -no-snapshot \
  -memory 2048)

# Current login sessions may not yet include the kvm group after usermod.
if command -v sg >/dev/null && getent group kvm | grep -q "${USER}"; then
  sg kvm -c "${EMU_CMD[*]}"
else
  "${EMU_CMD[@]}"
fi
