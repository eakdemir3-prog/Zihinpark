#!/bin/bash
# Emülatörde test APK'sını kurar ve belirli ekranların görüntüsünü alır.
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
  $T adb logcat -d -s ZP:D AndroidRuntime:E ActivityManager:E > "shots/$name.log" || true
  $T adb shell dumpsys activity processes | grep -i "anr\|notResponding" >> "shots/$name.log" || true
}
$T adb shell am start -S -n $ACT --ez zp_skip_tutorial true; sleep 10
for r in shop levels/logic_detective tab_profile; do shot "$r" 7; done
for g in nonogram memory_cards logic_detective; do shot "play/$g/1" 12; done
for g in nonogram word_hunt math_engineer shape_architect pattern_master logic_detective memory_cards river_crossing; do shot "play/$g/45" 15; done
ls -la shots
