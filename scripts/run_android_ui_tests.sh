#!/usr/bin/env bash
set -uo pipefail
# Disconnect only the disposable emulator; the CI host still downloads build inputs.
adb shell svc wifi disable
adb shell svc data disable
timeout --signal=TERM --kill-after=15s 12m gradle -p android -PqruParallelTest connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=br.com.qru.transito.ui.ReaderAndNewsUiTest,br.com.qru.transito.ui.OfflineLibraryDeviceTest --stacktrace
qru_test_exit=$?
adb logcat -d > android-ui-logcat.txt || true
rg 'QRU_UI_TEST|QRU_GUIDE_TREE' android-ui-logcat.txt | tail -n 240 || true
exit "$qru_test_exit"
