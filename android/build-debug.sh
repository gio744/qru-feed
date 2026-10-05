#!/usr/bin/env sh
set -eu
command -v gradle >/dev/null 2>&1 || { echo "Gradle not found"; exit 2; }
: "${ANDROID_HOME:?ANDROID_HOME is required}"
gradle testDebugUnitTest --stacktrace
gradle assembleDebug --stacktrace
sha256sum app/build/outputs/apk/debug/app-debug.apk > app/build/outputs/apk/debug/app-debug.apk.sha256
echo "APK: app/build/outputs/apk/debug/app-debug.apk"
