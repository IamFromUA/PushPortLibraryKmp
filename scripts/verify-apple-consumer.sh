#!/usr/bin/env bash
set -euo pipefail
repository="${1:?Pass an absolute path to the complete local Maven repository}"
bash gradlew --no-daemon -p examples/apple-consumer linkDebugFrameworkIosSimulatorArm64 "-Ppushport.repository=$repository"
python3 scripts/create-apple-consumer.py --output build/apple-consumer --version 0.0.1 \
  --framework "$PWD/examples/apple-consumer/build/bin/iosSimulatorArm64/debugFramework/ConsumerShared.framework"
xcodebuild -project build/apple-consumer/Consumer.xcodeproj -scheme Consumer \
  -configuration Debug -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath build/apple-derived ARCHS=arm64 CODE_SIGNING_ALLOWED=NO build
