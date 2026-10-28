from pathlib import Path
import json,re,hashlib
R=Path(__file__).resolve().parents[1]
data=R/'data/category-products.json';catalog=json.loads(data.read_text(encoding='utf-8'));ps=catalog['products']
assert len(ps)==201,'This migration is only for the original 201 image listings.'
baseline={p.relative_to(R).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in R.rglob('*') if p.is_file() and 'category-review' not in p.parts}
(R/'category-review/dedup-baseline.json').write_text(json.dumps(baseline))
(R/'category-review/products-before-dedup.json').write_text(json.dumps(catalog,indent=2,ensure_ascii=False),encoding='utf-8')
pairs=[(18,26),(63,84),(74,121),(79,104),(122,135),(151,168),(165,170),(166,176),(185,188)]
aliases={};removed=[]
for keep,drop in pairs:
 p,q=ps[keep-1],ps[drop-1]
 p.setdefault('alternateImages',[]).append(q)
 aliases[q['page']+'#'+q['reference']]=p['page']+'#'+p['reference']
 removed.append(q['reference'])
for p in ps:p['possibleVariants']=[ref for ref in p['possibleVariants'] if ref not in removed]
catalog['products']=[p for p in ps if p['reference'] not in removed]
catalog['productAliases']=aliases
data.write_text(json.dumps(catalog,indent=2,ensure_ascii=False),encoding='utf-8')
(R/'js/product-aliases.js').write_text("/* Preserve links to consolidated product references. */\n(function(){'use strict';const aliases="+json.dumps(aliases)+";function resolve(){const key=location.pathname.split('/').pop()+location.hash;if(aliases[key])location.replace(new URL(aliases[key],location.href).href)}window.addEventListener('hashchange',resolve);resolve()})();\n",encoding='utf-8')
p=R/'category-review/build_catalogue.py';s=p.read_text(encoding='utf-8')
s=re.sub(r'<dt>Size</dt>.*?<dd>\{confirmed\(\'stock\'\)\}</dd>','',s)
s=s.replace(" confirmed=lambda key: E(str(p[key])) if p.get(key) is not None else 'Confirm on WhatsApp'\n",'')
s=s.replace('<script src="js/site.js"></script></body>','<script src="js/site.js"></script><script src="js/product-aliases.js"></script></body>')
s=s.replace('product images','products').replace('10 categories · 201 products',"10 categories · '+str(len(products))+' products")
s=s.replace('Every image, individually referenced','Individually referenced styles')
s=s.replace('source images retained as individual products','unique catalogue listings; duplicate photos are retained in alternateImages')
s=s.replace('Do not rerun inspect.py: baseline.json records the pre-change site.','Run build_more.py afterwards to refresh the More tabs.')
p.write_text(s,encoding='utf-8')
p=R/'category-review/build_more.py';s=p.read_text(encoding='utf-8');start=s.index("for name in ['components/header.html'");end=s.index("source=(R/'collection.html')",start);s=s[:start]+s[end:]
s=s.replace('<script src="js/more.js"></script>','<script src="js/more.js"></script><script src="js/product-aliases.js"></script>');p.write_text(s,encoding='utf-8')
(R/'category-review/DEDUPLICATION.md').write_text('# Product deduplication\n\nRemoved nine duplicate listings, leaving 192 unique listings. Original photos and all 201 image records are preserved: nine alternate image records are nested under their retained products. Different colour, print and sleeve variants were retained.\n\nSize, Price, Fabric and Availability rows were removed from product cards. WhatsApp buttons and encoded messages remain.\n\nOld product links resolve to the retained reference:\n\n'+'\n'.join(f'- {old} → {new}' for old,new in aliases.items()),encoding='utf-8')
print('Consolidated',len(removed),'listings; remaining',len(catalog['products']))
