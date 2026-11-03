import json, sys, urllib.request, urllib.parse, os, re
Q=[("spor","football stadium night lights"),("spor2","stadium crowd"),("bilim","laboratory glassware"),("bilim2","chemistry laboratory flasks"),("sanat","paint palette brushes"),("sanat2","art gallery paintings museum"),("kultur","nazar boncuk"),("kultur2","Grand Bazaar Istanbul lamps")]
UA={'User-Agent':'ZihinParkBuild/1.0 (CI)'}
os.makedirs('out/c',exist_ok=True)
meta={}
for key,q in Q:
    url='https://commons.wikimedia.org/w/api.php?'+urllib.parse.urlencode({'action':'query','format':'json','generator':'search','gsrsearch':q+' filetype:bitmap','gsrnamespace':6,'gsrlimit':30,'prop':'imageinfo','iiprop':'url|extmetadata|size','iiurlwidth':1080})
    try: d=json.load(urllib.request.urlopen(urllib.request.Request(url,headers=UA),timeout=60))
    except Exception as e: print('fail',key,e); import time; time.sleep(3); continue
    pages=sorted(d.get('query',{}).get('pages',{}).values(),key=lambda p:p.get('index',99))
    n=0
    for p in pages:
        ii=(p.get('imageinfo') or [{}])[0]
        if ii.get('height',0)<700 or ii.get('width',0)<700: continue
        m=ii.get('extmetadata',{})
        lic=m.get('LicenseShortName',{}).get('value','')
        if not re.search(r'CC0|CC BY|Public domain',lic,re.I): continue
        if re.search(r'NC|ND',lic): continue
        art=re.sub('<[^>]+>','',m.get('Artist',{}).get('value',''))[:80]
        f=f'out/c/{key}_{n}.jpg'
        try:
            urllib.request.urlretrieve(ii['thumburl'],f) if False else open(f,'wb').write(urllib.request.urlopen(urllib.request.Request(ii['thumburl'],headers=UA),timeout=60).read())
        except Exception as e: print('dl',e); continue
        meta[f'{key}_{n}']={'title':p['title'],'artist':art.strip(),'license':lic,'page':ii.get('descriptionurl','')}
        n+=1
        if n>=6: break
    print(key,n); import time; time.sleep(2)
json.dump(meta,open('out/meta.json','w'),ensure_ascii=False,indent=1)
