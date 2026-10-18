#!/usr/bin/env bash
set -uo pipefail
# Disconnect only the disposable emulator; the CI host still downloads build inputs.
adb shell svc wifi disable
adb shell svc data disable
gradle -p android -PqruParallelTest connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=br.com.qru.transito.ui.ReaderAndNewsUiTest --stacktrace
qru_test_exit=$?
adb logcat -d > android-ui-logcat.txt || true
exit "$qru_test_exit"
