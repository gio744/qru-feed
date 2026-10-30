#!/usr/bin/env bash
set -uo pipefail
# Disconnect only the disposable emulator; the CI host still downloads build inputs.
adb shell svc wifi disable
adb shell svc data disable
# Compile and verify the complete document bundle before starting the test timeout.
# A document-integrity failure is a packaging failure, not a UI assertion failure.
gradle -p android :app:bundleNormPdf --stacktrace
bundle_exit=$?
if [ "$bundle_exit" -ne 0 ]; then
  echo "QRU_DOCUMENT_BUNDLE_FAILED: Official document verification failed before Android UI tests." >&2
  exit "$bundle_exit"
fi
timeout --signal=TERM --kill-after=15s 8m gradle -p android -PqruParallelTest connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=br.com.qru.transito.ui.ReaderAndNewsUiTest --stacktrace
qru_test_exit=$?
adb logcat -d > android-ui-logcat.txt || true
grep -E 'QRU_UI_TEST|QRU_GUIDE_TREE' android-ui-logcat.txt | tail -n 240 || true
exit "$qru_test_exit"
