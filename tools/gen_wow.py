import re
import json, random
from collections import Counter
common=json.load(open('common.json'))
# Kaba/uygunsuz ve çok işlevsel kelimeleri ele
BAD=set("""sik siki sikim sikik amk amına amcık göt götü orospu piç yarak yarrak taşak meme seks sevişmek kaltak fahişe ibne gavat pezevenk siktir sikmek sikiş dalyarak bok boktan kahpe puşt oç aq salak gerizekalı aptal ahmak geber gebermek öldürmek ölmek ölüm ceset intihar silah tabanca bomba uyuşturucu esrar eroin kokain alkol viski votka bira şarap sarhoş kan kanlı tecavüz kurban hey bay bayan""".split())
FUNC=set("""bir ben sen biz siz onlar bunu şunu ona bana sana bize size onu buna şuna ama var yok ile için gibi daha çok hiç her tüm şey kim tek mi mı mu mü evet hayır değil öyle böyle şöyle hadi peki eğer diye yani bile oldu olur olmak burada orada şurada burası orası nerede nereye nereden benden senden ondan bundan şundan onlar onlardan bunlar şunlar benim senin onun bizim sizin onların neden nasıl niye niçin kadar sonra önce şimdi artık belki çünkü sadece lütfen tamam merhaba efendim aslında elbette öyleyse yalnızca herhangi birileri birisi biri birkaç herkes hepsi hiçbir""".split())
TRU=str.maketrans('iı','İI')
def up(w): return w.translate(TRU).upper()
words={w:r for w,r in common.items() if w not in BAD and w not in FUNC}
def formable(base_cnt, w):
    c=Counter(w)
    return all(base_cnt[k]>=v for k,v in c.items())
ok=re.compile('^[abcçdefgğhıijklmnoöprsştuüvyz]+$')
LEM=set()
for f in ['words.txt','turkce_kelime_listesi.txt']:
    for l in open(f,encoding='utf-8'):
        x=l.strip()
        if ok.match(x) and 3<=len(x)<=8 and x not in BAD: LEM.add(x)
LEM=list(LEM)
wl=sorted(words, key=lambda w: words[w])
by_len={}
for w in wl: by_len.setdefault(len(w),[]).append(w)

def layout(ws, maxdim=9):
    # ws: uzunluktan kısaya; ilkini yatay koy
    grid={}  # (r,c)->ch
    placed=[]
    def can(w,r,c,d):
        dr,dc=(0,1) if d=='H' else (1,0)
        # öncesi/sonrası boş
        if grid.get((r-dr,c-dc)) or grid.get((r+dr*len(w),c+dc*len(w))): return -1
        inter=0
        for i,ch in enumerate(w):
            p=(r+dr*i,c+dc*i); g=grid.get(p)
            if g:
                if g!=ch: return -1
                inter+=1
            else:
                # yan komşular boş olmalı
                n1=(p[0]+dc,p[1]+dr); n2=(p[0]-dc,p[1]-dr)
                if grid.get(n1) or grid.get(n2): return -1
        return inter
    def bbox(extra=None):
        ks=list(grid.keys())+(extra or [])
        rs=[k[0] for k in ks]; cs=[k[1] for k in ks]
        return min(rs),max(rs),min(cs),max(cs)
    def put(w,r,c,d):
        dr,dc=(0,1) if d=='H' else (1,0)
        for i,ch in enumerate(w): grid[(r+dr*i,c+dc*i)]=ch
        placed.append((w,r,c,d))
    put(ws[0],0,0,'H')
    rest=ws[1:]
    for w in rest:
        best=None
        for (pr,pc),ch in list(grid.items()):
            for i,wc in enumerate(w):
                if wc!=ch: continue
                for d in 'HV':
                    r,c=(pr,pc-i) if d=='H' else (pr-i,pc)
                    k=can(w,r,c,d)
                    if k<1: continue
                    dr,dc=(0,1) if d=='H' else (1,0)
                    cells=[(r+dr*j,c+dc*j) for j in range(len(w))]
                    a,b,e,f=bbox(cells)
                    if b-a+1>maxdim or f-e+1>maxdim: continue
                    score=k*10-((b-a+1)*(f-e+1))*0.3+random.random()
                    if best is None or score>best[0]: best=(score,r,c,d)
        if best: put(w,best[1],best[2],best[3])
    a,b,e,f=bbox()
    return [(w,r-a,c-e,d) for w,r,c,d in placed], b-a+1, f-e+1

CHAP=[("İstanbul","🕌"),("Kapadokya","🎈"),("Pamukkale","♨️"),("Efes","🏛️"),("Nemrut","🗿"),("Uzungöl","🌲"),("Antalya","🏖️"),
      ("Paris","🗼"),("Roma","🏟️"),("Mısır","🐫"),("Japonya","🗾"),("New York","🗽"),("Rio","🌴"),("İzlanda","🌌"),("Machu Picchu","⛰️")]
def nletters(L):
    if L<=6: return 4
    if L<=40: return 5
    if L<=120: return 6
    if L<=230: return 7
    return 7 if L%3 else 8
def target(L):
    return min(4+L//25, 12)
random.seed(42)
used=set(); levels=[]
for L in range(1,301):
    n=nletters(L)
    cands=[w for w in by_len.get(n,[])[:900] if ''.join(sorted(w)) not in used]
    best=None
    for base in cands[:400]:
        bc=Counter(base)
        subs=[w for w in wl if 3<=len(w)<=n and w!=base and formable(bc,w)]
        if len(subs)+1 < min(target(L),4): continue
        # ağırlık: çeşitlilik için rastgele + sayıca uygun
        score=min(len(subs)+1,target(L)) - words[base]/3000 + random.random()*1.5
        if best is None or score>best[0]: best=(score,base,subs)
        if len(subs)+1>=target(L) and random.random()<0.25: break
    _,base,subs=best
    used.add(''.join(sorted(base)))
    pick=[base]+sorted(subs,key=lambda w:words[w])[:target(L)-1]
    pick=sorted(pick,key=len,reverse=True)
    for _ in range(30):
        pl,h,w_=layout(pick)
        if len(pl)>=min(len(pick),max(3,target(L)-2)): break
        random.shuffle(pick[1:])
    gridw={p[0] for p in pl}
    bonus=sorted((set(subs+[base])|{w for w in LEM if 3<=len(w)<=n and formable(Counter(base),w)})-gridw)[:60]
    letters=list(base); random.shuffle(letters)
    ch=(L-1)//20
    levels.append({"n":L,"ch":ch,"letters":up(''.join(letters)),
        "words":[{"t":up(w),"r":r,"c":c,"d":d} for w,r,c,d in pl],"rows":h,"cols":w_,
        "bonus":[up(b) for b in bonus]})
print([ (l['n'],l['letters'],len(l['words']),l['rows'],l['cols']) for l in levels[::30]])
json.dump({"chapters":[{"name":a,"emoji":b} for a,b in CHAP],"levels":levels},open('wow.json','w'),ensure_ascii=False,separators=(',',':'))
import os; print(os.path.getsize('wow.json'))
for l in levels[:3]+levels[150:152]+levels[298:]:
    print(l['n'],l['letters'],[x['t'] for x in l['words']],l['bonus'][:8])
