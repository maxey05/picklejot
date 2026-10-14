#!/usr/bin/env bash
# sys.boot_completed (all the emulator action waits for) fires while system_server is
# still dexopting on new images; Android 17 then drops binder calls ("Broken pipe").
# Wait until the package manager answers, then let the system settle.
adb wait-for-device
for _ in $(seq 1 90); do
  [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1 ] && adb shell pm path android > /dev/null 2>&1 && break
  sleep 2
done
sleep 20
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS > /dev/null 2>&1 || true
