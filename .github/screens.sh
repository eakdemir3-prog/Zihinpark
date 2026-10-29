#!/bin/bash
# Emülatörde test APK'sını kurar ve belirli ekranların görüntüsünü alır.
set -x
mkdir -p shots
adb install -r app-debug.apk
PKG=com.aistudio.zihinpark.trkm
ACT=$PKG/com.example.MainActivity
shot() {
  adb shell am start -S -n $ACT --es zp_route "$1" --ez zp_skip_tutorial true > /dev/null
  sleep ${2:-6}
  adb exec-out screencap -p > "shots/$(echo "$1" | tr '/' '_').png"
}
# İlk açılış (öğreticileri görüldü işaretler)
adb shell am start -S -n $ACT --ez zp_skip_tutorial true; sleep 8
adb exec-out screencap -p > shots/00_first_launch.png
for r in tab_home tab_games tab_records tab_profile shop levels/logic_detective levels/memory_cards; do shot "$r" 6; done
for g in logic_detective spatial_rotation nonogram cipher_breaker river_crossing shape_architect reverse_thinking \
  math_engineer detail_hunter path_finder number_chain word_hunt memory_lights reflex_arena color_stroop \
  pattern_master jigsaw_puzzle hidden_object number_merge logic_doors target_tap maze_escape memory_cards; do
  shot "play/$g/1" 7
done
for g in nonogram word_hunt math_engineer shape_architect pattern_master logic_detective; do shot "play/$g/45" 9; done
adb logcat -d -t 400 *:E > shots/logcat_errors.txt || true
ls -la shots
