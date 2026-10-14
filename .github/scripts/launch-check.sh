#!/usr/bin/env bash
# Run by android-emulator-runner, which executes `script:` one line at a time,
# so anything multi-line has to live in a file. Usage: launch-check.sh <api-level>
set -u
api=$1
apk=app/build/outputs/apk/debug/app-debug.apk
pkg=com.maxeydev.picklejot

fail() {
  echo "::error::$1 (API $api): $(echo "$2" | tr '\n' ' ' | cut -c1-400)"
  echo "::error::emulator: $(adb logcat -d 2>&1 | grep -E 'FATAL|ANR in|Out of memory|lowmemorykiller|system_server.*(died|crash)' | tail -8 | cut -c1-200 | tr '\n' '|')"
  adb logcat -d -t 300
  exit 1
}

bash .github/scripts/wait-ready.sh
out=$(adb install -r "$apk" 2>&1) || fail "adb install failed" "$out"
for try in 1 2 3; do
  out=$(adb shell am start -W -n "$pkg/.MainActivity" 2>&1) && break
  [ "$try" = 3 ] && fail "am start failed" "$out"
  sleep 15
done
sleep 5
# AC-1.19 — "it installed" is not "it launched". Assert the process is alive after
# start, so a crash-on-launch fails the job.
adb shell pidof "$pkg" > /dev/null || fail "app process not running after launch" "$out"
# And assert nothing crashed into the log on the way up.
crash=$(adb logcat -d -b crash)
if echo "$crash" | grep -q "$pkg"; then
  fail "App crashed on launch" "$(echo "$crash" | tail -50)"
fi
