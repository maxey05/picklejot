#!/usr/bin/env bash
# Run by android-emulator-runner (one script line at a time, see launch-check.sh).
# On failure, surfaces the Gradle and emulator evidence as annotations, since job
# logs are not readable without auth.
bash .github/scripts/wait-ready.sh
./gradlew :app:connectedDebugAndroidTest :data:connectedDebugAndroidTest :ui:connectedDebugAndroidTest \
  --continue --stacktrace 2>&1 | tee gradle.log
status=${PIPESTATUS[0]}
if [ "$status" -ne 0 ]; then
  echo "::error::gradle: $(grep -E 'FAILED|crashed|No connected|Unable to|INSTRUMENTATION' gradle.log | head -8 | cut -c1-200 | tr '\n' '|')"
  echo "::error::emulator: $(adb logcat -d 2>&1 | grep -E 'FATAL|ANR in|Out of memory|lowmemorykiller|system_server.*(died|crash)' | tail -8 | cut -c1-200 | tr '\n' '|')"
fi
exit "$status"
