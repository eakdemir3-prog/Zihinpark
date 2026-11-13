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
# ---- Mağaza görselleri: temiz durum çubuğu, sistem pencereleri kapalı ----
$T adb shell settings put global sysui_demo_allowed 1
$T adb shell am broadcast -a com.android.systemui.demo -e command enter >/dev/null
$T adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941 >/dev/null
$T adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false >/dev/null
$T adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 >/dev/null
$T adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false >/dev/null
store() {
  name="$1"
  adb logcat -c
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
store 1_home tab_home 12
store 0_lingo lingo/1 10
# ---- İngilizce ----
$T adb shell am start -S -n $ACT --es zp_route tab_home --ez zp_skip_tutorial true --ez zp_gift true --es zp_lang en > /dev/null
sleep 12
$T adb exec-out screencap -p > "shots/en_00_gift.png"
en() { store "$1" "$2" "$3"; mv "shots/store_$1.png" "shots/en_$1.png"; }
en 01_home tab_home 12
en 02_games tab_games 10
en 03_profile tab_profile 10
en 04_shop shop 10
en 05_records tab_records 10
en 06_path levels/sudoku 10
en 07_lingo lingo/1 10
en 08_lingomap lingo 10
en 09_wow wow/1 12
en 10_wowmap wow 10
en 11_bulmaca bulmaca 8
en 12_scramble play/word_scramble/45 10
en 13_detective play/logic_detective/45 10
en 14_reverse play/reverse_thinking/12 10
en 15_hunt play/word_hunt/20 10
en 16_code play/cipher_breaker/20 25
en 17_river play/river_crossing/5 10
en 18_stroop play/color_stroop/10 10
en 19_chain play/number_chain/20 10
en 20_doors play/logic_doors/20 10
en 21_sudoku play/sudoku/45 12
en 22_memory play/memory_cards/5 10
en 23_spatial play/spatial_rotation/10 10
en 24_hidden play/hidden_object/5 10
en 25_reflex play/target_tap/10 8
ls -la shots
