from pathlib import Path
from html.parser import HTMLParser
from urllib.parse import urlsplit,unquote
import json,re,hashlib
R=Path(__file__).resolve().parents[1]
archive_dirs=['backup-before-final-seo-verification-20260907-160428','seo-backup-20260907-130524','seo-review','final-seo-verification']
obsolete=['baseline.json','header-baseline.json','browser-and-delivery-checks.json','HEADER-UPDATE.md','verification.json','verify_catalogue.py','verify_header.py','check_local_delivery.py','group_header.py','update_header.py','inspect.py','unused-scan.json']
candidates=[p for d in archive_dirs for p in (R/d).rglob('*') if p.is_file()]
candidates+=[R/'category-review'/n for n in obsolete if (R/'category-review'/n).exists()]
candidates+=list((R/'category-review').glob('sheet-*.jpg'))
candidates += [R/p for p in json.loads((R/'category-review/unused-scan.json').read_text())['unusedAssets']]
relative=lambda p:p.relative_to(R).as_posix()
files=[{'path':relative(p),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in candidates]
allfiles=[p for p in R.rglob('*') if p.is_file() and not p.is_relative_to(R/'category-review') and p not in candidates]
baseline={relative(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in allfiles}
class Parse(HTMLParser):
 def __init__(self,s):super().__init__();self.refs=[];self.ids=set();self.feed(s)
 def handle_starttag(self,tag,attrs):
  d=dict(attrs)
  if 'id' in d:self.ids.add(d['id'])
  for key in ['href','src','data-site-href','data-site-src']:
   if d.get(key):self.refs.append(d[key])
  if d.get('srcset'):
   self.refs.extend(x.strip().split()[0] for x in d['srcset'].split(','))
pages={p.name:Parse(p.read_text(encoding='utf-8')) for p in R.glob('*.html')}
missing=[];checked=0
for name,doc in pages.items():
 for ref in doc.refs:
  u=urlsplit(ref)
  if u.scheme and u.scheme not in ['http','https']:continue
  if u.netloc and u.netloc not in ['www.veeracollections.com','veeracollections.com']:continue
  target=unquote(u.path).lstrip('/') or name
  if u.path=='/':target='index.html'
  p=R/target
  if not p.is_file() and (R/(target+'.html')).is_file():target+='.html';p=R/target
  checked+=1
  if not p.is_file():missing.append({'from':name,'link':ref})
  elif u.fragment and target in pages and unquote(u.fragment) not in pages[target].ids:missing.append({'from':name,'link':ref,'reason':'missing static anchor (may be dynamic)'})
plan={'root':str(R),'files':files,'removeDirectories':archive_dirs,'preserveHashes':baseline,'audit':{'htmlPages':len(pages),'localReferencesChecked':checked,'missingStaticReferences':missing,'removeFiles':len(files),'removeBytes':sum(x['bytes'] for x in files)}}
(R/'category-review/cleanup-plan.json').write_text(json.dumps(plan,indent=2))
print(json.dumps(plan['audit'],indent=2))
