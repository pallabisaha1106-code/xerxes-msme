import json,pathlib
from PIL import Image
root=pathlib.Path('.')
rows=[]
for p in root.joinpath('assets').rglob('*'):
 if p.suffix.lower() in ['.jpg','.jpeg','.png','.webp']:
  try:
   with Image.open(p) as im:
    im.verify()
   with Image.open(p) as im:
    rows.append({'file':str(p),'width':im.width,'height':im.height,'bytes':p.stat().st_size,'status':'PASS'})
  except Exception as e: rows.append({'file':str(p),'status':'FAIL','error':str(e)})
root.joinpath('audit-2026-09-10/image-decode.json').write_text(json.dumps(rows,indent=2),encoding='utf8')
print('Decoded',len(rows),'images; failures:',sum(x['status']=='FAIL' for x in rows))
