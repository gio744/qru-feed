#!/usr/bin/env sh
set -eu
if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
  echo "gradle-wrapper.jar is missing."
  echo "Run with trusted Gradle 8.9: gradle wrapper --gradle-version 8.9"
  exit 2
fi
./gradlew --version
