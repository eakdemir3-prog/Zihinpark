import json, random, sys, os
W=[("tarih","Tarih","🏛️"),("cografya","Coğrafya","🌍"),("hayvanlar","Hayvanlar","🦁"),("mutfak","Mutfak","🍲"),("bilim","Bilim","🔬"),
   ("spor","Spor","⚽"),("sanat","Sanat ve Müzik","🎨"),("uzay","Uzay","🚀"),("doga","Doğa","🌿"),("kultur","Gelenekler","🧿")]
D=os.path.dirname(os.path.abspath(__file__))
out=[]; total=0
for key,name,emo in W:
    pairs=[]; seen=set()
    for l in open(f'{D}/{key}.txt',encoding='utf-8'):
        l=l.strip()
        if not l or '|' not in l: continue
        c,a=l.rsplit('|',1); a=a.strip().replace(' ','')
        if a in seen or not (2<=len(a)<=11): continue
        seen.add(a); pairs.append((c.strip(),a))
    rng=random.Random(sum(map(ord,key))+7)
    secrets=[p for p in pairs if 5<=len(p[1])<=7]
    rng.shuffle(secrets)
    secrets.sort(key=lambda p:len(p[1]))  # kısa gizli kelimeler önce
    use={}
    puzzles=[]
    for sc,S in secrets:
        if len(puzzles)>=20: break
        rows=None
        for attempt in range(60):
            chosen=[]; ok=True
            for ch in S:
                cands=[(c,a) for c,a in pairs if a!=S and ch in a and use.get(a,0)<2 and a not in [x[1] for x in chosen]]
                if not cands: ok=False; break
                rng.shuffle(cands)
                cands.sort(key=lambda p:use.get(p[1],0))
                c,a=cands[0] if rng.random()<0.7 else cands[min(len(cands)-1,rng.randrange(3))]
                pos=[i for i,x in enumerate(a) if x==ch]; p=rng.choice(pos)
                chosen.append((c,a,p))
            if not ok: break
            col=max(p for _,_,p in chosen); width=max(col-p+len(a) for _,a,p in chosen)
            if width<=13: rows=chosen; break
        if not rows: continue
        for _,a,_ in rows: use[a]=use.get(a,0)+1
        use[S]=use.get(S,0)+2
        puzzles.append({"secret":S,"secretClue":sc,"rows":[{"c":c,"a":a,"p":p} for c,a,p in rows]})
    total+=len(puzzles)
    print(key,len(pairs),'puzzles',len(puzzles))
    out.append({"key":key,"name":name,"emoji":emo,"puzzles":puzzles})
json.dump({"worlds":out},open(sys.argv[1],'w'),ensure_ascii=False,separators=(',',':'))
print('total',total)
