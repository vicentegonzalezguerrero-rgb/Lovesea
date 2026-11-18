#!/usr/bin/env bash
# Instala la APK en el emulador y saca capturas de las pantallas principales.
set -x
OUT=capturas
mkdir -p $OUT
adb root || true
adb shell cmd alarm set-timezone America/Santiago || adb shell setprop persist.sys.timezone America/Santiago
adb shell settings put system time_12_24 24
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n cl.lovesea.mareas/.MainActivity
sleep 8
adb exec-out screencap -p > $OUT/1-lista.png

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

tap_text "Buscar playa o comuna"
sleep 1
adb shell input text "conchitas"
sleep 2
adb shell input keyevent 111  # cerrar teclado
sleep 1
adb exec-out screencap -p > $OUT/2-busqueda.png
tap_text "Las Conchitas"
sleep 3
adb exec-out screencap -p > $OUT/3-detalle.png
adb shell input swipe 540 1800 540 600 400
sleep 2
adb exec-out screencap -p > $OUT/4-detalle-abajo.png
adb shell input keyevent 4
sleep 1
adb shell "cmd uimode night yes"
sleep 3
adb exec-out screencap -p > $OUT/5-lista-oscuro.png
ls -la $OUT
