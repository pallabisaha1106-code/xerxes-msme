"""Local-only catalogue builder. Never changes source images or shared site files.
First run imports reviewed observations; later runs use data/category-products.json.
"""
from pathlib import Path
from PIL import Image,ImageOps
import json,hashlib,re,html
from urllib.parse import quote
ROOT=Path(__file__).resolve().parents[1]
SOURCE=Path('E:/Veera/New Photos')
REVIEW=ROOT/'category-review'
DATA=ROOT/'data/category-products.json'
ALLOWED={'collection.html','kurtis.html','plus-size-womens-clothing.html','ethnic-wear-pune.html','western-wear.html'}
PAGES={'Anarkali Dresses':'anarkali-dresses.html','Co-Ord Sets':'co-ord-sets.html','Formals':'formals.html','Indo-western':'indo-western.html','Kurtis':'kurtis.html','Night Dress':'night-dress.html','Punjabi Dress':'punjabi-dress.html','Sharara Sets':'sharara-sets.html','Skirt & Tops':'skirt-and-tops.html','Western Wear':'western-wear.html'}
E=html.escape
manifest=json.loads((REVIEW/'source-manifest.json').read_text())
if not DATA.exists():
 lines=(REVIEW/'observations.txt').read_text(encoding='utf-8').splitlines()
 assert len(lines)==len(manifest)==201
 products=[]
 for i,(m,line) in enumerate(zip(manifest,lines),1):
  colour,title,description=line.split('|')
  category=Path(m['source']).parent.name
  reference='VC-'+hashlib.sha256(Path(m['source']).name.encode()).hexdigest()[:12].upper()
  products.append(dict(reference=reference,source=m['source'],sourceSha256=m['sha256'],category=category,title=title,colour=colour,description=description,alt=f'{colour}: {title.lower()}. {description}',page=PAGES[category],image=f'assets/category-products/{reference.lower()}.webp',sizes=None,price=None,fabric=None,stock=None,care=None,includedItems=None,collection='New Arrivals',reviewNotes=[],possibleVariants=[]))
 groups=[[16,25],[18,26],[49,58],[63,84],[74,121],[75,128],[79,94,104],[122,135],[137,145],[140,144],[151,168],[152,153],[165,170],[166,176],[185,188]]
 for group in groups:
  for n in group: products[n-1]['possibleVariants']=[products[k-1]['reference'] for k in group if k!=n]
 uncertain={4:'The silhouette appears straighter than typical Anarkali styling; confirm folder classification.',38:'Draped trousers and a cropped jacket are visible; confirm Indo-western classification.',40:'Crop-top and wide-trouser styling may overlap with Co-Ord Sets.',41:'Flared dress silhouette may overlap with Anarkali Dresses.',51:'Flared silhouette may overlap with Anarkali Dresses.',56:'Flared silhouette may overlap with Anarkali Dresses.',57:'Flared silhouette may overlap with Anarkali Dresses.',59:'Tiered dress silhouette may overlap with Western Wear.',136:'Lower silhouette appears skirt-like; confirm sharara trousers versus skirt.',139:'Layered lower silhouette appears skirt-like; confirm sharara trousers versus skirt.',168:'Top and skirt are visible; current Western Wear folder overlaps with Skirt & Tops.'}
 for n,note in uncertain.items(): products[n-1]['reviewNotes'].append(note)
 DATA.parent.mkdir(exist_ok=True)
 DATA.write_text(json.dumps(dict(schemaVersion=1,products=products),indent=2,ensure_ascii=False),encoding='utf-8')
catalog_document=json.loads(DATA.read_text(encoding='utf-8'))
products=catalog_document['products']
assert len({p['reference'] for p in products})==len(products)
asset=ROOT/'assets/category-products';asset.mkdir(exist_ok=True)
for p in products:
 source=SOURCE/p['source']
 assert hashlib.sha256(source.read_bytes()).hexdigest()==p['sourceSha256'],str(source)
 target=ROOT/p['image']
 with Image.open(source) as im:
  im=ImageOps.exif_transpose(im).convert('RGB')
  for width,suffix in [(960,''),(480,'-480')]:
   dest=target.with_name(target.stem+suffix+'.webp')
   if not dest.exists():
    copy=im.copy();copy.thumbnail((width,width*2));copy.save(dest,'WEBP',quality=83,method=6)
  p['width'],p['height']=Image.open(target).size
DATA.write_text(json.dumps(catalog_document,indent=2,ensure_ascii=False),encoding='utf-8')
sitejs=(ROOT/'js/site.js').read_text(encoding='utf-8')
number=re.search(r"WHATSAPP_NUMBER='(\d+)'",sitejs).group(1)
domain=re.search(r'<link rel="canonical" href="(https://[^/]+)',(ROOT/'index.html').read_text(encoding='utf-8')).group(1)
template_path=REVIEW/'catalogue-template.html'
if not template_path.exists(): template_path.write_text((ROOT/'collection.html').read_text(encoding='utf-8'),encoding='utf-8')
template=template_path.read_text(encoding='utf-8')
head=template.split('</head>')[0]
head=head.replace('</title>','</title>')
head+='<link rel="stylesheet" href="css/pages/category-products.css"></head>'
def message(p):
 size=', '.join(p['sizes']) if p['sizes'] else 'Please advise available sizes'
 return f"Hello Veera Collections, I would like to check availability for:\n\nProduct: {p['title']}\nReference: {p['reference']}\nCategory: {p['category']}\nColour: {p['colour']}\nSize: {size}\nDetails: {p['description']}\nProduct link: {domain}/{p['page']}#{p['reference']}\n\nPlease confirm availability, available sizes, price, and fabric details. Thank you."
def card(p):
 ref=p['reference'];small=p['image'].replace('.webp','-480.webp')
 wa='https://wa.me/'+number+'?text='+quote(message(p),safe='')
 reviews='<p class="product-review">Category details available on request.</p>' if p['reviewNotes'] else ''
 variants='<p class="product-variants">Similar views: '+', '.join(f'<a href="{q["page"]}#{q["reference"]}">{q["reference"]}</a>' for q in products if q['reference'] in p['possibleVariants'])+'. Please ask whether these are variants.</p>' if p['possibleVariants'] else ''
 return f'''<article class="catalog-card" id="{ref}" aria-labelledby="title-{ref}"><div class="catalog-image"><a href="{p['image']}" aria-label="View larger image of {E(p['title'])}"><img src="{p['image']}" srcset="{small} 480w, {p['image']} {p['width']}w" sizes="(max-width:900px) 44vw, 280px" width="{p['width']}" height="{p['height']}" loading="lazy" decoding="async" alt="{E(p['alt'])}"></a></div><div class="catalog-info"><h3 id="title-{ref}">{E(p['title'])}</h3><p><a href="{p['page']}#{ref}" aria-label="Direct link to {E(p['title'])}, reference {ref}">{ref}</a></p><p>{E(p['description'])}</p><dl><dt>Category</dt><dd>{E(p['category'])}</dd><dt>Colour</dt><dd>{E(p['colour'])}</dd></dl>{reviews}{variants}<a class="catalog-action btn btn-whatsapp" href="{E(wa)}" target="_blank" rel="noopener noreferrer" aria-label="Check availability on WhatsApp for {E(p['title'])}, {ref} (opens in a new tab)">Check availability on WhatsApp</a></div></article>'''
def tiles(categories):
 result=''
 for category in categories:
  ps=[p for p in products if p['category']==category];p=ps[0]
  result+=f'<article class="catalog-card"><a class="catalog-image" href="{PAGES[category]}"><img src="{p["image"].replace(".webp","-480.webp")}" width="{p["width"]}" height="{p["height"]}" loading="lazy" alt="{E(category)} — {E(p["title"])}"></a><div class="catalog-info"><h3><a href="{PAGES[category]}">{E(category)}</a></h3><p>{len(ps)} products</p><a class="btn btn-outline" href="{PAGES[category]}">Explore category</a></div></article>'
 return '<div class="catalog-grid">'+result+'</div>'
def write_page(page,title,intro,body):
 if (ROOT/page).exists() and page not in ALLOWED and page not in PAGES.values(): raise ValueError(page)
 if page == 'collection.html':
  body = body.replace('loading="lazy"', 'loading="eager" fetchpriority="high"', 1)
  guide = re.search(r'<!-- collection-shopping-guide:start -->.*?<!-- collection-shopping-guide:end -->', (ROOT/page).read_text(encoding='utf-8'), re.S)
  if guide: body += guide.group(0)
 meta_description = f'Browse {title.lower()} at Veera Collections in Thergaon, PCMC. View outfit photos and confirm sizes, prices and availability on WhatsApp.' if page in PAGES.values() else intro
 h=re.sub(r'<title>.*?</title>',f'<title>{E(title)} | Veera Collections</title>',head)
 h=re.sub(r'<meta (name="description"|property="og:description"|name="twitter:description") content="[^"]*">',lambda m:f'<meta {m.group(1)} content="{E(meta_description)}">',h)
 h=re.sub(r'((?:property="og:title"|name="twitter:title") content=")[^"]*',lambda m:m.group(1)+E(title)+' | Veera Collections',h)
 h=h.replace('https://www.veeracollections.com/collection.html',domain+'/'+page)
 htmltext=h+f'<body class="category-products"><a class="skip-link" href="#main-content">Skip to content</a><div id="site-header"></div><main id="main-content"><section class="catalog-hero"><div class="container"><p class="eyebrow">Find your style</p><h1>{E(title)}</h1><p>{E(intro)}</p></div></section><section class="container catalog-section">'+body+'</section></main><div id="site-footer"></div><script src="js/site.js"></script><script src="js/product-aliases.js"></script></body></html>'
 (ROOT/page).write_text(htmltext,encoding='utf-8')
note='<p class="collection-note">Photographs show outfit styling. Confirm which pieces are included, along with size, price, fabric and availability, on WhatsApp.</p>'
for category,page in PAGES.items():
 ps=[p for p in products if p['category']==category]
 write_page(page,category,'Explore the collection and ask our team for confirmed product details.',f'<div class="catalog-heading"><h2>Explore {E(category)}</h2><p>{len(ps)} products</p></div>'+note+'<div class="catalog-grid">'+''.join(card(p) for p in ps)+'</div>')
write_page('collection.html','New Arrivals','Discover the latest photo collection by dress category. Confirm details for your chosen style on WhatsApp.','<div class="catalog-heading"><h2>Shop by category</h2><p>10 categories · '+str(len(products))+' products</p></div>'+tiles(PAGES)+'<div class="catalog-heading collection-products-heading"><h2>Explore the collection</h2><p>Individually referenced styles</p></div>'+note+''.join(f'<section class="collection-category"><h2><a href="{PAGES[c]}">{E(c)}</a></h2><div class="catalog-grid">'+''.join(card(p) for p in products if p['category']==c)+'</div></section>' for c in PAGES))
write_page('plus-size-womens-clothing.html','Find your fit','Browse by dress category and ask our team about sizes for your chosen reference. Product sizes are not yet confirmed.','<div class="catalog-heading"><h2>Explore the categories</h2></div><p>For M–5XL enquiries, choose a product and use its WhatsApp button. Our team will advise available sizes for that specific style.</p>'+tiles(PAGES))
ethnic=['Punjabi Dress','Kurtis','Anarkali Dresses','Sharara Sets','Indo-western']
write_page('ethnic-wear-pune.html','Ethnic','Explore Punjabi dress, kurtis, Anarkali dresses, sharara sets and Indo-western styles.','<div class="catalog-heading"><h2>Browse ethnic categories</h2></div>'+tiles(ethnic))
report=['# Category catalogue — owner review','',f'{len(products)} unique catalogue listings; duplicate photos are retained in alternateImages. No publishing performed.','', '## Pages and image counts','']
report += [f'- {c}: {sum(p["category"]==c for p in products)} images — {page}' for c,page in PAGES.items()]
report += ['', 'Collection: collection.html. Existing M–5XL and Ethnic URLs are category indexes; no garment sizes inferred. No nested New Arrivals directory, inventory, or non-product assets were present in the supplied folder at scan time.', '', '## Missing information', '', 'All products: confirmed sizes, price/currency, fabric, availability, care and included pieces. Fields are null in data/category-products.json; confirm with the owner before filling them. Visible components are descriptions of photographs, not promises of inclusion.', '', '## Classification review','']
report += [f'- {p["reference"]} ({p["category"]}): '+ ' '.join(p['reviewNotes']) for p in products if p['reviewNotes']]
report += ['', '## Possible variants / alternate presentations', '', 'Each image remains separate; these are visual similarities, not confirmed identical products.','']
report += [f'- {p["reference"]}: '+', '.join(p['possibleVariants']) for p in products if p['possibleVariants']]
report += ['', '## Maintenance', '', 'Edit data/category-products.json, then run category-review/build_catalogue.py using the bundled Python runtime. References are based on original filenames, remain stored in the catalogue and must not be regenerated when editing metadata. Run build_more.py afterwards to refresh the More tabs.', '', 'WhatsApp messages use the existing configured number and canonical domain. Public product URLs identify the final category page and anchor. They can be verified locally now; new public anchors become available only after an explicitly approved future publication.', '', 'Shared navigation, CSS, scripts, security settings and unrelated pages are untouched. New category styles apply only to replacement/new pages.']
(REVIEW/'OWNER-REVIEW.md').write_text('\n'.join(report),encoding='utf-8')
print('Built 10 category pages and 3 collection/index pages; retained',len(products),'images.')
