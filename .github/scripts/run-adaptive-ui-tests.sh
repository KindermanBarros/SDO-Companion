#!/usr/bin/env bash
set -euo pipefail

adb shell wm size 2560x1800
adb shell wm density 240
mkdir -p app/build/reports/adaptive-ui
trap 'adb pull /sdcard/Android/data/com.kinderman.sdo/files/adaptive-ui app/build/reports/adaptive-ui/screenshots || true' EXIT
ANDROID_SERIAL=emulator-5554 bash harness/scripts/run-android-instrumented-tests.sh \
  --class com.kinderman.sdo.ui.AdaptiveUiInstrumentedTest \
  --class com.kinderman.sdo.ui.CharacterSheetAccessInstrumentedTest \
  --output-dir app/build/reports/adaptive-ui
