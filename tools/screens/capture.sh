#!/usr/bin/env bash
# Instala la APK en el emulador y saca capturas de las pantallas principales.
set -x
OUT=capturas
rm -rf $OUT && mkdir -p $OUT
adb root || true
adb shell cmd alarm set-timezone America/Santiago || adb shell setprop persist.sys.timezone America/Santiago
adb shell settings put system time_12_24 24
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n cl.lovesea.mareas/.MainActivity
sleep 20   # deja que baje el pronóstico
adb exec-out screencap -p > $OUT/1-inicio.png

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
    print("no encontré", sys.argv[1])
PY
}

tap_text "Las Conchitas"
sleep 3
adb exec-out screencap -p > $OUT/2-playa.png
adb shell input swipe 540 1900 540 900 600
sleep 2
adb exec-out screencap -p > $OUT/3-playa-por-que.png
adb shell input swipe 540 1900 540 700 600
sleep 2
adb exec-out screencap -p > $OUT/4-playa-marea.png
adb shell input swipe 540 600 540 2000 300
adb shell input swipe 540 600 540 2000 300
sleep 1
tap_text "Natación"
sleep 2
adb exec-out screencap -p > $OUT/5-playa-natacion.png
tap_text "Surf"
sleep 2
adb exec-out screencap -p > $OUT/6-playa-surf.png
adb shell input keyevent 4
sleep 2
adb exec-out screencap -p > $OUT/7-inicio-surf.png
ls -la $OUT
