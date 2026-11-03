"""Bulmaca Dünyası verisi (CodyCross tarzı).
Her bulmaca: aynı uzunlukta (L) 10-15 satır, sorular karışık (tüm havuzlardan).
Bazı satırlardaki pembe kutular sırayla dünyanın konusundan bir gizli kelimeyi verir."""
import json, random, sys, os, glob
W=[("tarih","Tarih","🏛️"),("cografya","Coğrafya","🌍"),("hayvanlar","Hayvanlar","🦁"),("mutfak","Mutfak","🍲"),("bilim","Bilim","🔬"),
   ("spor","Spor","⚽"),("sanat","Sanat ve Müzik","🎨"),("uzay","Uzay","🚀"),("doga","Doğa","🌿"),("kultur","Gelenekler","🧿")]
D=os.path.dirname(os.path.abspath(__file__))
OK=set("ABCÇDEFGĞHIİJKLMNOÖPQRSŞTUÜVWXYZ")
BAD={"ZENCİ","KAKA","ÇİŞ"}
def norm(a): return a.strip().replace(' ','').replace('Â','A').replace('Î','İ').replace('Û','U').replace("'","")
def read(path):
    out=[]
    for l in open(path,encoding='utf-8'):
        l=l.strip()
        if '|' not in l: continue
        c,a=l.rsplit('|',1); a=norm(a); c=c.strip()
        if a in BAD or not set(a)<=OK or not c: continue
        up=c.replace('i','İ').replace('ı','I').upper().replace(' ','')
        if len(a)>=4 and a[:max(4,len(a)-2)] in up: continue
        out.append((c,a))
    return out
clues={}   # cevap -> soru listesi (tüm havuz)
for f in sorted(glob.glob(f'{D}/*.txt')):
    for c,a in read(f):
        lst=clues.setdefault(a,[])
        if c not in lst: lst.append(c)
byLen={}
for a in clues: byLen.setdefault(len(a),[]).append(a)
use={}
def rowsFor(i): return 10+(i*6)//20          # 10..15
LENS=[5,5,6,5,6,7,5,6,7,5,6,8,7,5,6,7,8,6,7,8]  # bulmaca sırasına göre harf sayısı
out=[]; total=0
for wi,(key,name,emo) in enumerate(W):
    rng=random.Random(sum(map(ord,key))*13+5)
    own=read(f'{D}/{key}.txt'); seen=set(); secrets=[]
    for c,a in own:
        if a in seen: continue
        seen.add(a)
        if 5<=len(a)<=10: secrets.append((c,a))
    rng.shuffle(secrets)
    puzzles=[]; si=0; wused=set()
    for i in range(20):
        N=rowsFor(i); L=LENS[i]
        built=None
        while si<len(secrets) and built is None:
            sc,S=secrets[si]; si+=1
            if len(S)>N: continue
            for attempt in range(40):
                slots=sorted(rng.sample(range(N),len(S)))
                chosen=[None]*N; taken={S}; ok=True
                order=list(range(N)); 
                for r in order:
                    ch=S[slots.index(r)] if r in slots else None
                    cands=[a for a in byLen.get(L,[]) if a not in taken and (ch is None or ch in a)]
                    if not cands: ok=False; break
                    rng.shuffle(cands); cands.sort(key=lambda a:(a in wused, use.get(a,0)))
                    a=cands[0] if rng.random()<0.75 else cands[min(len(cands)-1,rng.randrange(4))]
                    p=rng.choice([k for k,x in enumerate(a) if x==ch]) if ch else -1
                    taken.add(a); chosen[r]=(a,p)
                if ok: built=(sc,S,chosen); break
        if not built: print('!!',key,i); continue
        sc,S,chosen=built
        rows=[]
        for a,p in chosen:
            u=use.get(a,0); use[a]=u+1; wused.add(a)
            cl=clues[a][u%len(clues[a])]
            rows.append({"c":cl,"a":a,"p":p})
        puzzles.append({"secret":S,"secretClue":sc,"len":L,"rows":rows})
    total+=len(puzzles)
    print(key,'puzzles',len(puzzles),'secrets',len(secrets))
    out.append({"key":key,"name":name,"emoji":emo,"puzzles":puzzles})
json.dump({"worlds":out},open(sys.argv[1],'w'),ensure_ascii=False,separators=(',',':'))
import collections
print('total',total,'maxuse',max(use.values()),collections.Counter(use.values()))
