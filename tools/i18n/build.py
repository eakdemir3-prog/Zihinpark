"""en_*.py sözlüklerini birleştirir -> app/src/main/assets/i18n_en.json ve eksik anahtarları raporlar."""
import os, json, re, glob, importlib.util, sys
H = os.path.dirname(os.path.abspath(__file__))
EN = {}
for f in sorted(glob.glob(os.path.join(H, 'en_*.py'))):
    spec = importlib.util.spec_from_file_location(os.path.basename(f)[:-3], f); m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
    EN.update(m.EN)
exact, tpl = {}, []
for k, v in EN.items():
    if re.search(r'\{\d+\}', k): tpl.append([k, v])
    else: exact[k] = v
tpl.sort(key=lambda p: -len(p[0]))
out = os.path.join(H, '..', '..', 'app', 'src', 'main', 'assets', 'i18n_en.json')
json.dump({'exact': exact, 'tpl': tpl}, open(out, 'w', encoding='utf-8'), ensure_ascii=False, separators=(',', ':'))
keys = json.load(open(os.path.join(H, 'keys.json'), encoding='utf-8'))
miss = {}
for k, v in keys.items():
    if '\n' in k and len(k) > 200: continue
    if k in EN or k.strip() in EN: continue
    for f in v['files']: miss.setdefault(f, []).append(k)
print(len(exact), 'exact,', len(tpl), 'tpl;', sum(len(x) for x in miss.values()), 'missing')
if '-v' in sys.argv:
    for f, ks in sorted(miss.items()):
        print('##', f)
        for k in ks: print(repr(k))
else:
    for f, ks in sorted(miss.items(), key=lambda x: -len(x[1])): print(f'{len(ks):4} {f}')
