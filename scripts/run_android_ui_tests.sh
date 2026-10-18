#!/usr/bin/env bash
set -uo pipefail
gradle -p android -PqruParallelTest connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=br.com.qru.transito.ui.ReaderAndNewsUiTest --stacktrace
qru_test_exit=$?
adb logcat -d > android-ui-logcat.txt || true
exit "$qru_test_exit"
