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
  $T adb logcat -c
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
# ---- Mağaza görselleri: temiz durum çubuğu, sistem pencereleri kapalı ----
$T adb shell settings put global sysui_demo_allowed 1
$T adb shell am broadcast -a com.android.systemui.demo -e command enter >/dev/null
$T adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941 >/dev/null
$T adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false >/dev/null
$T adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 >/dev/null
$T adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false >/dev/null
store() {
  name="$1"
  $T adb logcat -c
  $T adb shell am start -S -n $ACT --es zp_route "$2" --ez zp_skip_tutorial true > /dev/null
  sleep ${3:-10}
  $T adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS > /dev/null
  sleep 1
  $T adb exec-out screencap -p > "shots/store_$name.png"
  $T adb logcat -d -s ZP:D AndroidRuntime:E > "shots/store_$name.log" || true
}
# Isınma: ilk açılış yavaş
$T adb shell am start -S -n $ACT --ez zp_skip_tutorial true; sleep 25
$T adb shell am start -S -n $ACT --es zp_route tab_home --ez zp_skip_tutorial true --ez zp_gift true --es zp_lang tr > /dev/null
sleep 12
$T adb exec-out screencap -p > "shots/store_0_gift.png"
# ---- Simge değişimi ----
PKG=com.eavisionlabs.zihinpark
$T adb shell am start -S -n $ACT --es zp_route tab_profile --ez zp_skip_tutorial true --es zp_lang tr --es zp_icon day > /dev/null
sleep 10
$T adb exec-out screencap -p > "shots/icon_0_profile.png"
$T adb shell input keyevent HOME; sleep 5
$T adb shell dumpsys package $PKG | grep -A4 -i "enabledComponents\|disabledComponents" > shots/icon_day.txt || true
$T adb shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER $PKG >> shots/icon_day.txt || true
$T adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1; sleep 10
$T adb exec-out screencap -p > "shots/icon_1_relaunch.png"
$T adb shell am start -S -n $ACT --es zp_route tab_home --ez zp_skip_tutorial true --es zp_icon night > /dev/null
sleep 8
$T adb shell input keyevent HOME; sleep 5
$T adb shell dumpsys package $PKG | grep -A4 -i "enabledComponents\|disabledComponents" > shots/icon_night.txt || true
$T adb shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER $PKG >> shots/icon_night.txt || true
ls -la shots
