from pathlib import Path
import re,html
R=Path(__file__).resolve().parents[1]
categories=[('new-arrivals','collection.html','New arrivals'),('sharara','sharara-sets.html','Sharara sets'),('formals','formals.html','Formals'),('night','night-dress.html','Night dress'),('skirts','skirt-and-tops.html','Skirt & Tops'),('indo-western','indo-western.html','Indo-western')]
source=(R/'collection.html').read_text(encoding='utf-8');head=source.split('</head>')[0]
head=re.sub(r'<title>.*?</title>','<title>More | Veera Collections</title>',head)
head=head.replace('https://www.veeracollections.com/collection.html','https://www.veeracollections.com/more.html')
head=re.sub(r'(<meta (?:name="description"|property="og:description"|name="twitter:description") content=")[^"]*',r'\g<1>Explore New arrivals, Sharara sets, Formals, Night dress, Skirt &amp; Tops and Indo-western.',head)
head=head.replace('New Arrivals | Veera Collections','More | Veera Collections')
tabs='<div class="more-tabs" aria-label="More categories">'+''.join(f'<a href="#{key}" id="tab-{key}" data-more-tab="{key}">{html.escape(label)}</a>' for key,page,label in categories)+'</div>'
panels=''
for key,page,label in categories:
 s=(R/page).read_text(encoding='utf-8');content=s.split('<section class="container catalog-section">',1)[1].rsplit('</section></main>',1)[0]
 # Keep public product URLs pointing to their category; prefix local repeated IDs.
 content=re.sub(r'\bid="([^"]+)"',lambda m:f'id="{key}-{m[1]}"',content)
 content=re.sub(r'aria-labelledby="([^"]+)"',lambda m:f'aria-labelledby="{key}-{m[1]}"',content)
 panels+=f'<section id="{key}" class="more-panel" aria-labelledby="tab-{key}"><p><a class="btn btn-outline" href="{page}">Open {html.escape(label)} page</a></p>{content}</section>'
result=head+'<link rel="stylesheet" href="css/pages/more.css"></head><body class="category-products"><a class="skip-link" href="#main-content">Skip to content</a><div id="site-header"></div><main id="main-content"><section class="catalog-hero"><div class="container"><p class="eyebrow">Find your style</p><h1>More</h1><p>Explore more of the Veera Collections range. Choose a category below.</p></div></section><section class="container catalog-section">'+tabs+panels+'</section></main><div id="site-footer"></div><script src="js/site.js"></script><script src="js/more.js"></script><script src="js/product-aliases.js"></script></body></html>'
(R/'more.html').write_text(result,encoding='utf-8')
print('Created More page with six category tabs; header order updated.')
