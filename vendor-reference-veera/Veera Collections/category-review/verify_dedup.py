from pathlib import Path
import json,re,hashlib
from html.parser import HTMLParser
from urllib.parse import urlsplit,parse_qs
R=Path(__file__).resolve().parents[1]
f=R/'data/category-products.json';data=json.loads(f.read_text(encoding='utf-8'));ps=data['products']
data['productAliases']={a['page']+'#'+a['reference']:p['page']+'#'+p['reference'] for p in ps for a in p.get('alternateImages',[])}
f.write_text(json.dumps(data,indent=2,ensure_ascii=False),encoding='utf-8')
images=[p for p in ps]+[a for p in ps for a in p.get('alternateImages',[])]
assert len(ps)==192 and len(images)==201
manifest=json.loads((R/'category-review/source-manifest.json').read_text())
assert {p['source'] for p in images}=={m['source'] for m in manifest}
for p in images:assert (R/p['image']).is_file()
class Parse(HTMLParser):
 def __init__(self,s):super().__init__();self.cards=[];self.links=[];self.feed(s)
 def handle_starttag(self,t,a):
  d=dict(a)
  if t=='article' and d.get('id'):self.cards.append(d['id'])
  if t=='a' and 'catalog-action' in d.get('class',''):self.links.append(d['href'])
pages={p['page'] for p in ps}|{'collection.html','more.html'}
buttons=0
for page in pages:
 s=(R/page).read_text(encoding='utf-8');doc=Parse(s)
 assert not re.search(r'<dt>(Size|Price|Fabric|Availability)</dt>',s)
 assert len(doc.cards)==len(set(doc.cards))
 for link in doc.links:
  u=urlsplit(link);assert u.netloc=='wa.me' and u.path=='/919921959748'
  msg=parse_qs(u.query)['text'][0];ref=msg.split('Reference: ')[1].split('\n')[0]
  assert ref in {p['reference'] for p in ps};buttons+=1
 if page=='collection.html':assert len(doc.cards)==192
 elif page!='more.html':assert len(doc.cards)==sum(p['page']==page for p in ps)
baseline=json.loads((R/'category-review/dedup-baseline.json').read_text())
for name in ['index.html','visit-us.html','components/header.html','components/footer.html','js/site.js','css/header.css','_headers','_redirects']:
 assert hashlib.sha256((R/name).read_bytes()).hexdigest()==baseline[name],name
for old,new in data['productAliases'].items():
 page,ref=new.split('#');assert 'id="'+ref+'"' in (R/page).read_text(encoding='utf-8')
result={'uniqueListings':len(ps),'mergedDuplicates':9,'preservedSourceImages':len(images),'verifiedWhatsAppButtons':buttons,'removedMetadataRows':True,'headerFooterHomeVisitSecurityUnchanged':True,'categoryCounts':{c:sum(p['category']==c for p in ps) for c in sorted({p['category'] for p in ps})}}
(R/'category-review/dedup-verification.json').write_text(json.dumps(result,indent=2))
print(json.dumps(result,indent=2))
