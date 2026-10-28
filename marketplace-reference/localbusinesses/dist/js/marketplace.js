/* Neighbourly: no dependencies, tracking, storage or backend. */
const { businesses, categories, config } = window.marketplaceData;
const pincodeAreaData = window.pincodeAreaData || {};
const $ = selector => document.querySelector(selector);
let activeCategory = 'All';
let activeCity = '';
const listingSingular = config.listingSingular || 'shop';
const listingPlural = config.listingPlural || 'shops';
const escapeHTML = value => String(value ?? '').replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
const safeURL = value => { try { const url = new URL(value, location.href); return ['http:', 'https:', 'file:'].includes(url.protocol) ? url.href : ''; } catch { return ''; } };
const whatsappURL = (number, message) => /^\d{8,15}$/.test(String(number)) ? `https://wa.me/${number}?text=${encodeURIComponent(message)}` : '';
// Contact URLs are independent of local image paths.
function contactURL(value){if(typeof value!=='string'||!/^https?:\/\//i.test(value)||/[\s\\\u0000-\u001f\u007f]/.test(value))return '';try{const u=new URL(value);return ['http:','https:'].includes(u.protocol)&&u.hostname&&!u.username&&!u.password?value:'';}catch{return '';}}
const contactPlatforms = [
  [
    "instagram",
    "Instagram",
    "<rect x=\"3\" y=\"3\" width=\"18\" height=\"18\" rx=\"5\"/><circle cx=\"12\" cy=\"12\" r=\"4\"/><circle cx=\"17.5\" cy=\"6.5\" r=\"1\" fill=\"currentColor\" stroke=\"none\"/>"
  ],
  [
    "facebook",
    "Facebook",
    "<path d=\"M14 22v-9h3l.5-4H14V7c0-1.2.4-2 2-2h2V1.5A22 22 0 0 0 15 1c-3 0-5 1.8-5 5v3H7v4h3v9Z\" fill=\"currentColor\" stroke=\"none\"/>"
  ],
  [
    "youtube",
    "YouTube",
    "<rect x=\"2\" y=\"5\" width=\"20\" height=\"14\" rx=\"4\"/><path d=\"m10 9 6 3-6 3Z\" fill=\"currentColor\" stroke=\"none\"/>"
  ],
  [
    "googleMaps",
    "Google Maps",
    "<path d=\"M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0Z\"/><circle cx=\"12\" cy=\"10\" r=\"2.5\"/>"
  ],
  [
    "whatsapp",
    "WhatsApp",
    "<path d=\"M21 11.5a9 9 0 0 1-13.4 7.9L3 21l1.5-4.7A9 9 0 1 1 21 11.5Z\"/><path d=\"m8 7 1.6 2.8-1.1 1.1c.8 1.6 2 2.8 3.7 3.5l1.1-1.2 2.7 1.5c-.2 1.6-1.5 2.3-3 1.8-3.5-1.1-6.2-3.8-7.1-7.1-.3-1.1.5-2.2 2.1-2.4Z\" fill=\"currentColor\" stroke=\"none\"/>"
  ],
  [
    "website",
    "Website",
    "<circle cx=\"12\" cy=\"12\" r=\"9\"/><ellipse cx=\"12\" cy=\"12\" rx=\"4\" ry=\"9\"/><path d=\"M3 12h18M5 6.5h14M5 17.5h14\"/>"
  ]
];
function renderContactLinks(business){
 const buttons=contactPlatforms.map(([key,name,icon])=>{
 const href=contactURL(business.links?.[key]);if(!href)return '';
 const label=business.businessName+(key==='website'?' website':' on '+name);
 return '<a class="contact-link" href="'+escapeHTML(href)+'" target="_blank" rel="noopener noreferrer" aria-label="'+escapeHTML(label)+'" title="'+escapeHTML(label)+'"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">'+icon+'</svg></a>';
 }).join('');return buttons?'<div class="card-actions">'+buttons+'</div>':'';
}
function productPageURL(business) {
 const page=business.productPage;
 return page?.enabled === true && typeof page.url === 'string' && /^[a-z0-9][a-z0-9-]*\.html$/.test(page.url) ? page.url : '';
}
function renderProductPageLink(business) {
 const url=productPageURL(business);
 return url ? '<a class="view-products-link" href="'+escapeHTML(url)+'" aria-label="View all products from '+escapeHTML(business.businessName)+'">View all products ↗</a>' : '';
}
if ($('#category-list')) {
const categoriesNode = $('#category-list');
categoriesNode.innerHTML = [{name:'All',icon:'▦'}, ...categories].map(category => `<button class="category-button" data-category="${escapeHTML(category.name)}" aria-pressed="${category.name === 'All'}"><span aria-hidden="true">${escapeHTML(category.icon)}</span>${escapeHTML(category.name === 'All' ? 'All ' + listingPlural : category.name)}</button>`).join('');
function addOption(select,value,label=value){const option=document.createElement('option');option.value=value;option.textContent=label;select.append(option);}
const allCatalogueAreas=[...new Set(businesses.map(business=>business.area))].sort();
const allPincodes=[...new Set([...Object.keys(pincodeAreaData),...businesses.map(business=>business.pincode)])].sort();
for(const value of allPincodes)addOption($('#pincode'),value);
function mappedAreas(pincode){return Array.isArray(pincodeAreaData[pincode])?pincodeAreaData[pincode]:[];}
function refreshAreas(pincode,preferred=''){
 const areaSelect=$('#area');areaSelect.innerHTML='';addOption(areaSelect,'','All areas');
 if(!pincode){for(const area of allCatalogueAreas)addOption(areaSelect,area);}
 else{
  const entries=mappedAreas(pincode);const represented=new Set();
  for(const entry of entries){addOption(areaSelect,entry.label);represented.add(entry.label);for(const match of entry.matches||[])represented.add(match);}
  for(const area of businesses.filter(business=>business.pincode===pincode).map(business=>business.area).sort())if(!represented.has(area)){addOption(areaSelect,area);represented.add(area);}
 }
 areaSelect.value=[...areaSelect.options||[]].some(option=>option.value===preferred)?preferred:'';
}
refreshAreas('');
function areaMatches(business){
 const selected=$('#area').value;if(!selected)return true;
 const entry=mappedAreas($('#pincode').value).find(item=>item.label===selected);
 return business.area===selected||Boolean(entry?.matches?.includes(business.area));
}
function applyPincode(value,{renderResults=true}={}){
 const pincode=/^[1-9][0-9]{5}$/.test(value)?value:'';
 if(pincode&&![...($('#pincode').options||[])].some(option=>option.value===pincode))addOption($('#pincode'),pincode);
 $('#pincode').value=pincode;$('#pincode-entry').value=pincode;activeCity='';refreshAreas(pincode);
 if(renderResults)render();
}
function matches(business) {
  const terms = $('#search').value.toLocaleLowerCase().trim().split(/\s+/).filter(Boolean);
  const haystack = [business.businessName, business.businessType, business.address, business.area, business.pincode, ...[...business.products,...(business.productPage?.enabled ? business.productPage.products || [] : [])].map(p => p.name)].join(' ').toLocaleLowerCase();
  const businessCity=business.city||config.defaultCity||'';
  return (activeCategory === 'All' || business.businessType === activeCategory) && (!activeCity || businessCity === activeCity) && areaMatches(business) && (!$('#pincode').value || business.pincode === $('#pincode').value) && terms.every(term => haystack.includes(term));
}
function render() {
  if(window.saveDiscoverySelection)window.saveDiscoverySelection({pincode:$('#pincode').value,city:activeCity,category:activeCategory});
  const filtered = businesses.filter(matches);
  $('#result-count').textContent = `${filtered.length} ${filtered.length === 1 ? listingSingular : listingPlural} to discover${activeCategory !== 'All' ? ` · ${activeCategory}` : ''}`;
  $('#empty').hidden = filtered.length > 0;
  $('#business-grid').innerHTML = filtered.map(business => {

    return `<article class="business-card" data-shop="${escapeHTML(business.id)}"><img class="shop-image" src="${escapeHTML(safeURL(business.shopPhoto || 'images/placeholder.svg'))}" alt="${escapeHTML(business.shopPhotoAlt || business.businessName + " — illustrative shop preview")}" loading="lazy" width="400" height="247"><div class="card-content"><span class="business-type">${escapeHTML(business.businessType)}</span><h3>${escapeHTML(business.businessName)}</h3><p class="location">⌖ &nbsp;${escapeHTML(business.area)} · ${escapeHTML(business.pincode)}</p><div class="product-gallery">${business.products.slice(0,3).map((product,index) => `<button class="product-button" data-business="${escapeHTML(business.id)}" data-product="${index}" aria-label="Preview ${escapeHTML(product.name)}"><img src="${escapeHTML(safeURL(product.image || 'images/placeholder.svg'))}" alt="${escapeHTML(product.name)}" loading="lazy" width="120" height="120"></button>`).join('')}</div><p class="product-caption">${business.products.length ? 'A little look inside · Tap to explore' : 'Product previews coming soon'}</p><p class="address">${escapeHTML(business.address)}</p>${renderContactLinks(business)}${renderProductPageLink(business)}</div></article>`;
  }).join('');
  document.querySelectorAll('#business-grid img').forEach(img => img.addEventListener('error', () => { img.src = 'images/placeholder.svg'; }, {once:true}));
}
function reset() { activeCategory = 'All'; activeCity = ''; $('#search').value = ''; $('#pincode').value = ''; $('#pincode-entry').value = ''; refreshAreas(''); updateCategories(); render(); }
function updateCategories() { categoriesNode.querySelectorAll('button').forEach(button => button.setAttribute('aria-pressed', button.dataset.category === activeCategory)); }
categoriesNode.addEventListener('click', event => { const button = event.target.closest('button'); if (!button) return; activeCategory = button.dataset.category; updateCategories(); render(); });
$('#search').addEventListener('input',render);
$('#search-form').addEventListener('submit',event => { event.preventDefault(); render(); $('#marketplace').scrollIntoView({behavior:matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth'}); });
$('#area').addEventListener('change',()=>{activeCity='';render();});
$('#pincode').addEventListener('change',()=>applyPincode($('#pincode').value));
$('#pincode-entry').addEventListener('input',()=>{
 const input=$('#pincode-entry');input.value=input.value.replace(/\D/g,'').slice(0,6);
 if(input.value.length===6)applyPincode(input.value);
 else if($('#pincode').value){$('#pincode').value='';activeCity='';refreshAreas('');render();}
});
$('#reset').addEventListener('click',reset); $('#empty-reset').addEventListener('click',reset);
$('#business-grid').addEventListener('click',event => {
  const button = event.target.closest('.product-button'); if (!button) return;
  const business = businesses.find(item => item.id === button.dataset.business); const product = business.products[Number(button.dataset.product)];
  $('#preview-image').src = safeURL(product.image || 'images/placeholder.svg'); $('#preview-image').alt = product.name;
  $('#preview-title').textContent = product.name; $('#preview-shop').textContent = business.businessName; $('#lightbox').showModal();
});
$('#preview-image').addEventListener('error', () => { if (!$('#preview-image').src.endsWith('/placeholder.svg')) $('#preview-image').src = 'images/placeholder.svg'; });
document.querySelectorAll('dialog').forEach(dialog => { dialog.querySelector('.dialog-close').addEventListener('click',()=>dialog.close()); dialog.addEventListener('click',event => { if(event.target===dialog) { const rect = dialog.getBoundingClientRect(); if(event.clientX<rect.left || event.clientX>rect.right || event.clientY<rect.top || event.clientY>rect.bottom) dialog.close(); } }); });
$('.dismiss').addEventListener('click',()=>$('#contact-notice').close());
const adminURL = whatsappURL(config.adminWhatsapp, config.submissionMessage || 'Hello! I would like to list my business on Neighbourly.\nBusiness name:\nBusiness type:\nWhatsApp number:\nComplete address:\nArea:\nPincode:\nGoogle Maps location:\nI will also send shop and product photos.');
document.querySelectorAll('.admin-link').forEach(link => { link.href = adminURL || '#'; if(adminURL) { link.target='_blank'; link.rel='noopener noreferrer'; } else link.classList.add('demo-contact'); });
document.addEventListener('click',event => { if(event.target.closest('.demo-contact')) { event.preventDefault(); $('#contact-notice h2').textContent = event.target.closest('.admin-link') ? 'Business submissions open soon' : 'This is a sample shop'; $('#contact-notice').showModal(); } });
$('.menu-button').addEventListener('click',()=> { const open = $('#navigation').classList.toggle('open'); $('.menu-button').setAttribute('aria-expanded',open); });
$('#navigation').addEventListener('click',event => { if(event.target.closest('a')) { $('#navigation').classList.remove('open'); $('.menu-button').setAttribute('aria-expanded','false'); } });
$('#year').textContent = new Date().getFullYear();
render();
if(window.initializeDiscoveryWelcome)window.initializeDiscoveryWelcome({businesses,categories,kind:listingPlural,welcomeCategories:config.welcomeCategories,alwaysShow:config.alwaysShowDiscovery,onApply(selection){
 applyPincode(selection.pincode,{renderResults:false});activeCity=selection.city;activeCategory=selection.category;$('#search').value='';updateCategories();render();
}});
}
