from pathlib import Path
import json,re,hashlib
R=Path(__file__).resolve().parents[1]
runtime=list(R.glob('*.html'))+list((R/'css').rglob('*.css'))+list((R/'js').rglob('*.js'))+list((R/'components').glob('*.html'))+list((R/'data').glob('*.json'))+[R/'site.webmanifest',R/'sitemap.xml',R/'_redirects',R/'robots.txt']
text='\n'.join(p.read_text(encoding='utf-8') for p in runtime)
assets=list((R/'assets').rglob('*'))
unused=[p for p in assets if p.is_file() and p.name not in text]
summary={'unusedAssets':[str(p.relative_to(R)).replace('\\','/') for p in unused], 'unusedAssetBytes':sum(p.stat().st_size for p in unused),'folders':{}}
for p in R.iterdir():
 if p.is_dir():
  files=[f for f in p.rglob('*') if f.is_file()]
  summary['folders'][p.name]={'files':len(files),'bytes':sum(f.stat().st_size for f in files)}
(R/'category-review/unused-scan.json').write_text(json.dumps(summary,indent=2))
print(json.dumps(summary,indent=2))
