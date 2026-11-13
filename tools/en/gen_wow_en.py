"""English Word Land data -> app/src/main/assets/wow_en.json"""
import re, json, random, os, sys
from collections import Counter
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from en_words_lib import *  # bad, DICT, LOWER, NAMEISH, top_n_list, zipf_frequency
ok = re.compile('^[a-z]+$')
top = top_n_list('en', 40000)
def fine(w):
    return ok.match(w) and 3 <= len(w) <= 8 and w in LOWER and not bad(w) and w not in NAMEISH
FUNC = set("the and for are but not you all any can had her was one our out his has him how its may who did yes let she too use etc via per also than then them they this that what with from have were been will your into".split())
ODD = set('abu dow las lee ide ana una ute udo ont ens ers ess tis nos ism ima eds ted tex sec rem sep feb oct nov dec jan aug apr mar jun jul thu tue wed fri sat sun mon inc ltd est vol ave fig ref min max app des los les del san por que sur dos tres une hai hoy sam ole'.split())
words = {}
for i, w in enumerate(top):
    if fine(w) and w not in FUNC and w not in words and w not in ODD and zipf_frequency(w, 'en') >= (4.2 if len(w) == 3 else 3.4): words[w] = i
up = str.upper
def formable(base_cnt, w):
    c = Counter(w)
    return all(base_cnt[k] >= v for k, v in c.items())
LEM = [w for w in DICT if ok.match(w) and 3 <= len(w) <= 8 and not bad(w) and zipf_frequency(w, 'en') >= 2.0]
wl = sorted(words, key=lambda w: words[w])
by_len = {}
for w in wl: by_len.setdefault(len(w), []).append(w)
def layout(ws, maxdim=10):
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

CHAP=[("Istanbul","🕌"),("Cappadocia","🎈"),("Pamukkale","♨️"),("Ephesus","🏛️"),("Nemrut","🗿"),("Uzungöl","🌲"),("Antalya","🏖️"),
      ("Paris","🗼"),("Rome","🏟️"),("Egypt","🐫"),("Japan","🗾"),("New York","🗽"),("Rio","🌴"),("Iceland","🌌"),("Machu Picchu","⛰️")]
OUT=os.path.join(os.path.dirname(os.path.abspath(__file__)),'..','..','app','src','main','assets','wow_en.json')
def nletters(L):
    if L<=25: return 5
    if L<=80: return 6
    if L<=190: return 7
    return 7 if L%3 else 8
def target(L):
    return min(7+L//30, 15)
random.seed(42)
used=set(); levels=[]
for L in range(1,301):
    n=nletters(L)
    cands=[w for w in by_len.get(n,[])[:900] if ''.join(sorted(w)) not in used]
    best=None
    for base in cands[:900]:
        bc=Counter(base)
        subs=[w for w in wl if 3<=len(w)<=n and w!=base and formable(bc,w)]
        if len(subs)+1 < min(target(L),6): continue
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
json.dump({"chapters":[{"name":a,"emoji":b} for a,b in CHAP],"levels":levels},open(OUT,'w'),separators=(',',':'))
import os; print(os.path.getsize(OUT))
for l in levels[:3]+levels[150:152]+levels[298:]:
    print(l['n'],l['letters'],[x['t'] for x in l['words']],l['bonus'][:8])
