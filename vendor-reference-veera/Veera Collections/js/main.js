const WHATSAPP_NUMBER='919921959748';
const products=[
['Rose Pink Floral Kurta Set','A vivid kurta and trouser set with a soft floral dupatta.','assets/catalog/rose-pink-floral-kurta-set.jpeg'],
['Midnight Belted Co-ord Set','A polished shirt-and-trouser co-ord with a softly cinched waist.','assets/catalog/black-belted-coord-set.jpeg'],
['Maroon Strappy Maxi Dress','A fluid maxi with delicate crossover straps and a graceful fall.','assets/catalog/maroon-strappy-maxi-dress.jpeg'],
['Black Satin Maxi Dress','A timeless full-length dress with a sweetheart neckline.','assets/catalog/black-satin-maxi-dress.jpeg'],
['Peacock Floral Kurta Set','A statement kurta set with floral panels and a flowing dupatta.','assets/catalog/peacock-floral-kurta-set.jpeg'],
['Mint Embroidered Anarkali Set','An embroidered anarkali with a sheer drape and detailed borders.','assets/catalog/mint-embroidered-anarkali-set.jpeg'],
['Rose Gold Kurta Set','A soft kurta and trouser set with subtle gold accents.','assets/catalog/rose-gold-kurta-set.jpeg'],
['Midnight Print Kaftan Dress','An easy kaftan dress with a relaxed drape and gathered waist.','assets/catalog/midnight-floral-kaftan-dress.jpeg']];
const grid=document.querySelector('#productGrid');
if(grid){products.forEach(([name,description,image])=>{const card=document.createElement('article');card.className='product-card';const msg=encodeURIComponent(`Hi Veera Collections, I saw the ${name} on your website. Please share current sizes, price and store availability.`);card.innerHTML=`<img src="${image}" alt="${name}" width="640" height="853" loading="lazy" decoding="async"><div class="product-info"><h3>${name}</h3><p>${description}</p><p class="status">● Check current availability</p><a class="track" data-event="whatsapp_click" href="https://wa.me/${WHATSAPP_NUMBER}?text=${msg}" target="_blank" rel="noopener noreferrer">WhatsApp to check availability</a></div>`;grid.append(card)})}
const menu=document.querySelector('.menu'),nav=document.querySelector('#nav');menu?.addEventListener('click',()=>{const open=nav.classList.toggle('open');menu.setAttribute('aria-expanded',String(open))});nav?.querySelectorAll('a').forEach(a=>a.addEventListener('click',()=>{nav.classList.remove('open');menu.setAttribute('aria-expanded','false')}));
document.querySelectorAll('.track').forEach(link=>link.addEventListener('click',()=>{const eventName=link.dataset.event;if(typeof window.gtag==='function')window.gtag('event',eventName,{link_url:link.href});window.dataLayer=window.dataLayer||[];window.dataLayer.push({event:eventName,link_url:link.href})}));
