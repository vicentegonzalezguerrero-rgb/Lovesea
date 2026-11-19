#!/usr/bin/env bash
# Instala la APK en el emulador y saca capturas de las pantallas principales.
set -x
OUT=capturas
rm -rf $OUT && mkdir -p $OUT
adb root || true
adb shell cmd alarm set-timezone America/Santiago || adb shell setprop persist.sys.timezone America/Santiago
adb shell settings put system time_12_24 24
tap_text() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null
  adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null
  python3 - "$1" <<'PY'
import re, sys, subprocess
xml = open('/tmp/ui.xml', encoding='utf-8').read()
for m in re.finditer(r'<node [^>]*>', xml):
    n = m.group(0)
    if f'text="{sys.argv[1]}"' in n:
        x1, y1, x2, y2 = map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n).groups())
        subprocess.run(["adb", "shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2)])
        break
else:
    pass
PY
}

dismiss() {
  # El emulador recién encendido a veces muestra "isn't responding" del launcher.
  adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null
  tap_text "Wait"
  tap_text "Close app"
}
shot() { dismiss; sleep 1; adb exec-out screencap -p > "$OUT/$1.png"; }

sleep 30   # deja que el sistema termine de arrancar
dismiss
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n cl.lovesea.mareas/.MainActivity
sleep 20   # deja que baje el pronóstico
shot 1-inicio

tap_text "Buscar playa o comuna"
sleep 1
adb shell input text "conchitas"
sleep 1
adb shell input keyevent 111
sleep 1
tap_text "Las Conchitas"
sleep 3
shot 2-playa
adb shell input swipe 540 1900 540 900 600
sleep 2
shot 3-playa-por-que
adb shell input swipe 540 1900 540 700 600
sleep 2
shot 4-playa-marea
adb shell input swipe 540 600 540 2000 300
adb shell input swipe 540 600 540 2000 300
sleep 1
tap_text "Natación"
sleep 2
shot 5-playa-natacion
tap_text "Surf"
sleep 2
shot 6-playa-surf
adb shell input keyevent 4
sleep 2
shot 7-inicio-surf
ls -la $OUT
