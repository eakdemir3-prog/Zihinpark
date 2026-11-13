"""Kotlin kaynaklarındaki kullanıcıya görünen Türkçe metinleri çıkarır -> tools/i18n/keys.json
Şablon değişkenleri ($x, ${...}) {0},{1}... yer tutucularına çevrilir."""
import re, os, json, sys
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'app', 'src', 'main', 'java', 'com', 'example')
SKIP_FILES = {'WordData.kt', 'Turkish.kt', 'NameFilter.kt', 'I18n.kt', 'I18nText.kt'}
lit = re.compile(r'(?<![\w$])"((?:[^"\\]|\\.)*)"')

def to_template(s):
    out, i, n = [], 0, 0
    while i < len(s):
        c = s[i]
        if c == '\\' and i + 1 < len(s):
            nx = s[i+1]
            out.append({'n': '\n', 't': '\t', '"': '"', '\\': '\\', '$': '$', "'": "'"}.get(nx, nx)); i += 2; continue
        if c == '$' and i + 1 < len(s):
            if s[i+1] == '{':
                depth, j = 0, i + 1
                while j < len(s):
                    if s[j] == '{': depth += 1
                    elif s[j] == '}':
                        depth -= 1
                        if depth == 0: break
                    j += 1
                out.append('{%d}' % n); n += 1; i = j + 1; continue
            m = re.match(r'[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)*', s[i+1:])
            if m:
                out.append('{%d}' % n); n += 1; i += 1 + len(m.group(0)); continue
        out.append(c); i += 1
    return ''.join(out), n

TRCH = re.compile(r'[çğıöşüÇĞİÖŞÜâÂ]')
TRW = re.compile(r'\b(ve|bir|bu|ile|için|da|de|mi|ne|var|yok|en|Seviye|seviye|Oyna|oyna|Oyunlar|oyun|Oyun|kelime|Kelime|Elmas|elmas|Profil|Ayarlar|Rozet|Sonsuz|Mod|Yeni|Kapat|Tamam|Geri|Kaydet|Sil|Al|Git|Harika|Puan|Sayı|sayı|Hedef|hedef|Kart|kart|Renk|renk|Blok|blok|Tur|tur|Zor|Kolay|Orta|Usta|Efsane|Lig|lig|Bul|bul|Seç|Yap|yap|Tema|Ses|ses|Titreşim|Neon|Ana|Sayfa|Kazan|kazan|Dene|dene|Hata|hata|Doğru|Yanlış|Süre|süre|Puan|Hamle|hamle|Durdur|Devam|devam|Başla|Bitti|Hazır|Kilitli|Açık|Kapalı)\b')

def wanted(t):
    if not re.search(r'[A-Za-zÇĞİÖŞÜçğıöşü]', t): return False
    if re.fullmatch(r'[a-z0-9_./:%-]+', t): return False          # anahtar / rota
    if t.startswith(('http', 'zp_', 'android.', 'com.', 'market:', 'mailto:', 'ca-app-pub')): return False
    if re.fullmatch(r'[A-Za-z0-9_]+', t) and not TRW.search(t) and not TRCH.search(t): return False
    return bool(TRCH.search(t) or TRW.search(t))

keys = {}
for dp, _, fs in os.walk(ROOT):
    for f in fs:
        if not f.endswith('.kt') or f in SKIP_FILES: continue
        src = open(os.path.join(dp, f), encoding='utf-8').read()
        # Logları ve annotation'ları atla
        for m in lit.finditer(src):
            line_start = src.rfind('\n', 0, m.start()) + 1
            line = src[line_start:src.find('\n', m.end())]
            if 'Log.' in line or line.strip().startswith(('//', '*', '@')) or 'Key("' in line or 'PreferencesKey(' in line: continue
            t, n = to_template(m.group(1))
            if wanted(t):
                keys.setdefault(t, {'n': n, 'files': set()})['files'].add(f)
out = {k: {'n': v['n'], 'files': sorted(v['files'])} for k, v in sorted(keys.items())}
json.dump(out, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'keys.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=0)
print(len(out), 'anahtar;', sum(1 for v in out.values() if v['n']), 'şablon')
