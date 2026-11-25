#!/usr/bin/env bash
set -euo pipefail
repository="${1:?Pass an absolute path to the complete local Maven repository}"
bash gradlew --no-daemon -p examples/apple-consumer linkDebugFrameworkIosSimulatorArm64 "-Ppushport.repository=$repository"
package=(--version 0.1.1)
if [[ -n "${PUSHPORT_KMP_LOCAL_APPLE_PACKAGE:-}" ]]; then
  package=(--local-package "$PUSHPORT_KMP_LOCAL_APPLE_PACKAGE")
fi
python3 scripts/create-apple-consumer.py --output build/apple-consumer "${package[@]}" \
  --framework "$PWD/examples/apple-consumer/build/bin/iosSimulatorArm64/debugFramework/ConsumerShared.framework"
xcodebuild -project build/apple-consumer/Consumer.xcodeproj -scheme Consumer \
  -configuration Debug -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath build/apple-derived ARCHS=arm64 CODE_SIGNING_ALLOWED=NO build

if [[ -n "${PUSHPORT_KMP_TEST_SIMULATOR:-}" ]]; then
  device="$PUSHPORT_KMP_TEST_SIMULATOR"
  bundle=dev.pushport.publication.consumer
  run_id=$(uuidgen | tr '[:lower:]' '[:upper:]')
  xcrun simctl bootstatus "$device" -b
  xcrun simctl install "$device" build/apple-derived/Build/Products/Debug-iphonesimulator/Consumer.app
  xcrun simctl launch --terminate-running-process "$device" "$bundle" --verify-consent "$run_id"
  result="$(xcrun simctl get_app_container "$device" "$bundle" data)/tmp/$run_id"
  for ((attempt=0; attempt<30; attempt++)); do
    [[ -f "$result" ]] && break
    sleep 1
  done
  [[ -f "$result" ]] || { echo 'The KMP app did not report its startup result.' >&2; exit 1; }
  cat "$result"
  grep -qx 'Configured with consent pending' "$result"
fi
