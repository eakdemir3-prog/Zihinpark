#!/bin/bash
# Emülatörde test APK'sını kurar ve ekran görüntüsü alır.
set -x
mkdir -p shots
adb install -r app-debug.apk
PKG=com.eavisionlabs.zihinpark
ACT=$PKG/com.example.MainActivity
T="timeout 25"
shot() {
  name="$(echo "$1" | tr '/' '_')"
  adb logcat -c
  $T adb shell am start -S -n $ACT --es zp_route "$1" --ez zp_skip_tutorial true > /dev/null
  sleep ${2:-6}
  $T adb exec-out screencap -p > "shots/$name.png"
  $T adb logcat -d -s ZP:D AndroidRuntime:E > "shots/$name.log" || true
}
# Uygulama içinde bir yazıya dokun (uiautomator ile)
tap_text() {
  $T adb shell uiautomator dump /sdcard/ui.xml > /dev/null
  $T adb pull /sdcard/ui.xml /tmp/ui.xml > /dev/null
  b=$(python3 - "$1" <<'PY'
import re,sys
x=open('/tmp/ui.xml',encoding='utf-8').read()
for m in re.finditer(r'<node [^>]*>', x):
    n=m.group(0)
    if ('text="%s"'%sys.argv[1]) in n or ('content-desc="%s"'%sys.argv[1]) in n:
        b=re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',n)
        if b:
            a=list(map(int,b.groups())); print((a[0]+a[2])//2,(a[1]+a[3])//2); break
PY
)
  [ -n "$b" ] && $T adb shell input tap $b
}
$T adb shell am start -S -n $ACT --ez zp_skip_tutorial true; sleep 25
adb exec-out screencap -p > shots/00_home.png
for r in tab_games tab_records tab_profile shop; do shot "$r" 12; done
for g in color_sort block_blast neon_match sudoku neon_connect; do shot "play/$g/1" 9; done
for g in color_sort block_blast neon_match sudoku neon_connect; do shot "play/$g/45" 12; done
# İpucu testi
for g in number_chain spatial_rotation logic_detective; do
  shot "play/$g/1" 8
  tap_text "İpucu"; sleep 1.2
  $T adb exec-out screencap -p > "shots/hint_$g.png"
done
ls -la shots
