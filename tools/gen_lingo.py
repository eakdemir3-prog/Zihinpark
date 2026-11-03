import json,re,random
exec(open('gen_wow2.py').read().split("def formable")[0])  # BAD, FUNC, words, up
ok=re.compile('^[abcçdefgğhıijklmnoöprsştuüvyz]+$')
LEM=set()
for f in ['words.txt','turkce_kelime_listesi.txt']:
    for l in open(f,encoding='utf-8'):
        x=l.strip()
        if ok.match(x) and len(x) in (5,6) and x not in BAD: LEM.add(x)
# WordData isimleri (kategorili, temiz)
src=open('/home/claude/Zihinpark/app/src/main/java/com/example/game/puzzles/WordData.kt',encoding='utf-8').read()
wd=set(w for w in re.findall(r'"([A-ZÇĞİÖŞÜ]+)"',src))
def low(w): return w.replace('I','ı').replace('İ','i').lower()
wdl={low(w) for w in wd if len(w) in (5,6)}
# Cevaplar: sık kelimeler (fiil çekimlerini ve işlevsel kelimeleri at) + WordData
bad_end=('acak','ecek','mış','miş','muş','müş','yor','dır','dir','dur','dür','tır','tir','ken','sın','sin','lar','ler','dan','den','tan','ten','nın','nin','mız','miz','nız','niz')
A=set(l.strip() for l in open('words.txt',encoding='utf-8')); B=set(l.strip() for l in open('turkce_kelime_listesi.txt',encoding='utf-8'))
BOTH=A&B
NOT={'gücün','kaçar','soruma','gibisi','fransa','değiş','demin','koyma','korkma','ağlama','yerine','uzunca','meğer'}
def good(w):
    if w not in BOTH or w in NOT: return False
    if w in BAD or w in FUNC: return False
    if len(w)==6 and w.endswith(bad_end): return False
    if len(w)==5 and w.endswith(('yor','dır','dir','lar','ler','dan','den','mış','miş')): return False
    return True
ranked=sorted([w for w in words if len(w) in (5,6) and good(w)],key=lambda w:words[w])
a5=[w for w in ranked if len(w)==5][:900]
a6=[w for w in ranked if len(w)==6][:700]
for w in wdl:
    if good(w):
        (a5 if len(w)==5 else a6).append(w)
a5=list(dict.fromkeys(a5)); a6=list(dict.fromkeys(a6))
random.seed(7); random.shuffle(a5); random.shuffle(a6)
valid=sorted(LEM|set(a5)|set(a6)|{w for w in words if len(w) in (5,6)})
json.dump({'a5':[up(w) for w in a5],'a6':[up(w) for w in a6],'valid':[up(w) for w in valid]},open('lingo.json','w'),ensure_ascii=False,separators=(',',':'))
print(len(a5),len(a6),len(valid)); print(a5[:40]); print(a6[:30])
