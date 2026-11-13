#!/usr/bin/env bash
set -euo pipefail

adb shell wm size 2560x1800
adb shell wm density 240
mkdir -p app/build/reports/adaptive-ui
trap 'adb pull /sdcard/Android/data/com.kinderman.sdo/files/adaptive-ui app/build/reports/adaptive-ui/screenshots || true' EXIT
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest --stacktrace
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r \
  -e class com.kinderman.sdo.ui.AdaptiveUiInstrumentedTest,com.kinderman.sdo.ui.CharacterSheetAccessInstrumentedTest \
  com.kinderman.sdo.test/androidx.test.runner.AndroidJUnitRunner \
  | tee app/build/reports/adaptive-ui/instrumentation.log
grep -Eq '^OK \([1-9][0-9]* tests?\)' app/build/reports/adaptive-ui/instrumentation.log
