from pathlib import Path
from PIL import Image,ImageOps,ImageDraw
import json
R=Path(__file__).resolve().parents[1]
ps=json.loads((R/'data/category-products.json').read_text())['products']
groups=[[16,25],[18,26],[49,58],[63,84],[74,121],[75,128],[79,94,104],[122,135],[137,145],[140,144],[151,168],[152,153],[165,170],[166,176],[185,188]]
for start in range(0,len(groups),5):
 sheet=Image.new('RGB',(1200,2250),'white');d=ImageDraw.Draw(sheet)
 for row,group in enumerate(groups[start:start+5]):
  for col,n in enumerate(group):
   p=ps[n-1];im=Image.open(R/p['image']);im.thumbnail((390,410));x=col*400;y=row*450;sheet.paste(im,(x,y));d.text((x,y+415),f'{n}: {p["reference"]}',fill='black')
 sheet.save(R/f'category-review/duplicate-review-{start//5+1}.jpg')
