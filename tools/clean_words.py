"""Kelime oyunlarındaki uygunsuz / hassas kelimeleri cevaplardan çıkarır (Play içerik derecelendirmesi için).
Kullanım: python3 tools/clean_words.py  (assets klasöründeki json'ları yerinde günceller)"""
import json, os, re
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'app', 'src', 'main')
BAD = set("""
AMCIK SİK SİKİŞ SİKMEK SİKİK GÖT GÖTÜ GÖTLÜ OROSPU PİÇ YARRAK YARAK TAŞAK TAŞŞAK KAKA ÇİŞ BOK SIÇ SIÇMA SIÇAN_NO OSURUK OSUR
ZENCİ İBNE PUŞT SÜRTÜK KAHPE FAHİŞE ESKORT GENELEV PEZEVENK KEVAŞE GAVUR KAFİR ÇİNGENE
PENİS VAJİNA MEME MEMESİ ÇIPLAK SEKS SEKSİ CİNSEL CİNSİ ŞEHVET AZGIN EROTİK PORNO TECAVÜZ TACİZ ÇÜK DÖL SPERM REGL ADET_NO PRESERVATİF KONDOM
APTAL SALAK GERİZEKALI AHMAK BUDALA DANGALAK ŞİŞKO KÖTÜRÜM SAKAT_NO ÖZÜRLÜ ŞAPŞAL ENAYİ KEPAZE REZİL HAYVANCA
ÖLDÜR ÖLÜM ÖLÜMÜ ÖLÜ ÖLÜLER CESET LEŞ KATİL CİNAYET İNTİHAR ASILMA BOĞMA BOĞDU DAYAK DÖVMEK DÖVDÜ BIÇAKLA KURŞUNLA
SİLAH TABANCA TÜFEK BOMBA TERÖR TERÖRİST SAVAŞÇI_NO KATLİAM SOYKIRIM İŞKENCE
UYUŞTURUCU ESRAR KOKAİN EROİN HAP_NO ALKOL ALKOLİK RAKI ŞARAP BİRA VİSKİ VOTKA SARHOŞ İÇKİ İÇKİCİ KUMAR KUMARBAZ BAHİS SİGARA TÜTÜN NARGİLE
PİSLİK KUSMUK KUSMA SALYA SÜMÜK İSHAL
""".split())
BAD = {w for w in BAD if not w.endswith('_NO')}

def bad(w): return w in BAD

def lingo():
    p = os.path.join(ROOT, 'assets', 'lingo.json'); d = json.load(open(p, encoding='utf-8'))
    out = {}
    for k in ('a5', 'a6'):
        before = len(d[k]); d[k] = [w for w in d[k] if not bad(w)]; out[k] = before - len(d[k])
    json.dump(d, open(p, 'w', encoding='utf-8'), ensure_ascii=False, separators=(',', ':'))
    return out

def wow():
    p = os.path.join(ROOT, 'assets', 'wow.json'); d = json.load(open(p, encoding='utf-8'))
    removed = []
    for lv in d['levels']:
        keep = [w for w in lv['words'] if not bad(w['t'])]
        if len(keep) != len(lv['words']):
            removed += [(lv['n'], w['t']) for w in lv['words'] if bad(w['t'])]
            if len(keep) < 3: raise SystemExit(f"seviye {lv['n']} çok az kelimeyle kaldı")
            lv['words'] = keep
        lv['bonus'] = [b for b in lv.get('bonus', []) if not bad(b)]
    json.dump(d, open(p, 'w', encoding='utf-8'), ensure_ascii=False, separators=(',', ':'))
    return removed

def check_bulmaca():
    d = json.load(open(os.path.join(ROOT, 'assets', 'bulmaca.json'), encoding='utf-8'))
    return [r['a'] for w in d['worlds'] for p in w['puzzles'] for r in p['rows'] if bad(r['a'])] + \
           [p['secret'] for w in d['worlds'] for p in w['puzzles'] if bad(p['secret'])]

def check_kotlin():
    hits = []
    for dp, _, fs in os.walk(os.path.join(ROOT, 'java')):
        for f in fs:
            if f.endswith('.kt'):
                for w in re.findall(r'"([A-ZÇĞİÖŞÜ]{3,})"', open(os.path.join(dp, f), encoding='utf-8').read()):
                    if bad(w): hits.append((f, w))
    return hits

if __name__ == '__main__':
    print('lingo çıkarılan', lingo())
    print('wow çıkarılan', wow())
    print('bulmaca', check_bulmaca())
    print('kotlin', check_kotlin())
