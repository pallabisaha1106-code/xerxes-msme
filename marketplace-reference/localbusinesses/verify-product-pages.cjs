
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict'),path=require('node:path');
class Element{
 constructor(){this.children=[];this.listeners={};this.attrs={};this.hidden=true;this.classList={toggle:()=>true};}
 append(...x){this.children.push(...x);}
 before(node){this.previous=node;}
 replaceChildren(...nodes){this.children=nodes;}
 removeAttribute(key){delete this[key];delete this.attrs[key];}
 setAttribute(k,v){this.attrs[k]=v;}
 addEventListener(k,fn){this.listeners[k]=fn;}
 querySelector(){return this.closeButton||(this.closeButton=new Element());}
 showModal(){this.open=true;}
 close(){this.open=false;}
}
function setup(id='veera',disabled=false,route='veera-collection.html'){
 const nodes=new Map(),get=k=>{if(k==='#category-list')return null;if(!nodes.has(k))nodes.set(k,new Element());return nodes.get(k);};
 const context=vm.createContext({window:{},document:{body:{dataset:{productShop:id}},querySelector:get,createElement:()=>new Element()},location:{href:'http://localhost/'+route,pathname:'/'+route},URL});
 vm.runInContext(fs.readFileSync('dist/data/businesses.js','utf8'),context);
 if(disabled)context.window.marketplaceData.businesses.find(b=>b.id==='veera').productPage.enabled=false;
 vm.runInContext(fs.readFileSync('dist/js/marketplace.js','utf8'),context);
 vm.runInContext(fs.readFileSync('dist/js/shop-products.js','utf8'),context);
 return {get,run:s=>vm.runInContext(s,context),data:context.window.marketplaceData};
}
const app=setup();
assert.equal(app.get('#catalogue-content').hidden,false);
assert.equal(app.get('#catalogue-grid').children.length,12);
assert.equal(app.get('#catalogue-count').textContent,'12 products');
const veera=app.data.businesses.find(b=>b.id==='veera');
assert.equal(veera.productPage.products.length,9);
for(const p of [...veera.products,...veera.productPage.products])assert.ok(fs.existsSync(path.join('dist',p.image)));
assert.ok(app.run("renderProductPageLink(businesses.find(b=>b.id==='veera'))").includes('veera-collection.html'));
for(const b of app.data.businesses.filter(b=>!['veera','mateshwari-super-mart'].includes(b.id)))assert.equal(app.run('renderProductPageLink('+JSON.stringify(b)+')'),'');
for(const [id,disabled,route] of [['computer-town',false,'veera-collection.html'],['veera',true,'veera-collection.html'],['missing',false,'veera-collection.html'],['veera',false,'computer-town.html']]){
 const result=setup(id,disabled,route);assert.equal(result.get('#catalogue-unavailable').hidden,false);assert.equal(result.get('#catalogue-grid').children.length,0);
}
const card=app.get('#catalogue-grid').children[3];card.children[0].listeners.click();
assert.equal(app.get('#catalogue-preview').open,true);
assert.equal(app.get('#catalogue-preview-title').textContent,'Purple floral set');
assert.ok(app.get('#catalogue-preview-code').textContent.includes('VC-IW-01'));
app.get('#catalogue-preview').querySelector().listeners.click();assert.equal(app.get('#catalogue-preview').open,false);
const html=fs.readFileSync('dist/veera-collection.html','utf8');
for(const [,ref] of html.matchAll(/(?:src|href)="((?:css|js|data|images)\/[^"]+|(?:index|wholesalers)\.html)(?:#[^"]*)?"/g))assert.ok(fs.existsSync(path.join('dist',ref)),ref);
assert.ok(html.includes('<title>Veera Collection Products | Neighbourly</title>'));
console.log('PASS: Opt-in catalogue links; 12 products including nine supplied images; disabled, missing and mismatched pages unavailable; product dialog open/close; local assets and metadata.');


for(const item of app.get('#catalogue-grid').children){
 const enquiry=item.children[3];assert.equal(enquiry.className,'product-enquiry');
 const url=new URL(enquiry.href);assert.equal(url.hostname,'wa.me');assert.equal(url.pathname,'/919921959748');
 assert.ok(url.searchParams.get('text').includes('Product: '+item.children[1].textContent));
 assert.ok(url.searchParams.get('text').includes('Ref: '+item.children[2].textContent.split(' · Ref ')[1]));
 assert.equal(enquiry.target,'_blank');assert.equal(enquiry.rel,'noopener noreferrer');
}
assert.equal(app.run("productEnquiryURL({enquiryWhatsapp:''},{name:'Test'},'R1')"),'');
assert.equal(app.run("productEnquiryURL({enquiryWhatsapp:'https://chat.whatsapp.com/test'},{name:'Test'},'R1')"),'');
const other=app.run("productEnquiryURL({businessName:'Another Shop',enquiryWhatsapp:'+91 90000 00000'},{name:'Dress & jacket'},'OTHER-01')");
assert.equal(new URL(other).pathname,'/919000000000');
assert.ok(new URL(other).searchParams.get('text').includes('Dress & jacket'));
assert.ok(new URL(other).searchParams.get('text').includes('OTHER-01'));
console.log('PASS: all 12 enquiry buttons use Veera’s number and exact name/reference; per-shop routing; invalid numbers omitted; URL encoding.');


const mart=setup('mateshwari-super-mart',false,'mateshwari-super-mart.html');
assert.equal(mart.get('#catalogue-grid').children.length,50);
const m=mart.data.businesses.find(b=>b.id==='mateshwari-super-mart');
assert.deepEqual(Array.from(m.products,p=>p.name),['Aashirvaad Atta','India Gate Basmati Rice','Tata Salt']);
assert.equal(m.links.googleMaps,'https://maps.app.goo.gl/DPejmeNAb5a2m9jB8?g_st=ac');
assert.equal((mart.get('#catalogue-contact').innerHTML.match(/class="contact-link"/g)||[]).length,1);
assert.ok(mart.run('renderProductPageLink('+JSON.stringify(m)+')').includes('mateshwari-super-mart.html'));
const all=[...m.products,...m.productPage.products];assert.equal(new Set(all.map(p=>p.id)).size,50);
for(const [i,p] of all.entries()){
 assert.ok(fs.existsSync(path.join('dist',p.image)));
 assert.ok(fs.statSync(path.join('dist',p.image)).size>1000);
 assert.deepEqual([p.name,p.packSize,p.mrp],require('./mateshwari-catalogue-fixture.json')[i]);
 const item=mart.get('#catalogue-grid').children[i],url=new URL(item.children.find(c=>c.className==='product-enquiry').href);
 assert.equal(url.pathname,'/917666069942');
 assert.equal(url.searchParams.get('text'),'Hello Krishna Mart, I found this product on Neighbourly.\nProduct: '+p.name+'\nRef: '+p.id+'\nPlease share the price, available sizes and availability.');
 item.children[0].listeners.click();assert.ok(mart.get('#catalogue-preview-code').textContent.includes(p.id));
}
assert.ok(fs.existsSync(path.join('dist',m.shopPhoto)));
console.log('PASS: Krishna 50 unique products, exact enquiry messages and number, supplied images, pack sizes, MRPs, previews and Maps-only contacts.');

const groceryGrid=mart.get('#catalogue-grid'),cartPanel=groceryGrid.previous;
assert.ok(cartPanel);assert.equal(app.get('#catalogue-grid').previous,undefined);
assert.ok(app.get('#catalogue-grid').children.every(c=>!c.children.some(x=>x.className==='product-add-cart')));
assert.equal(groceryGrid.children.filter(c=>c.children.some(x=>x.className==='product-add-cart')).length,50);
const add=i=>groceryGrid.children[i].children.find(x=>x.className==='product-add-cart').listeners.click();
add(0);add(0);add(1);
const sendCart=cartPanel.children[3].children[0];
let cartURL=new URL(sendCart.href),message=cartURL.searchParams.get('text');
assert.equal(cartURL.pathname,'/917666069942');
assert.ok(message.includes('Aashirvaad Atta (5 kg) | Ref: MSM-001 | Quantity: 2'));
assert.ok(message.includes('India Gate Basmati Rice (1 kg) | Ref: MSM-002 | Quantity: 1'));
assert.equal(cartPanel.children[2].children.length,2);
const controls=()=>cartPanel.children[2].children[0].children[1];
controls().children[0].listeners.click();
assert.ok(new URL(sendCart.href).searchParams.get('text').includes('MSM-001 | Quantity: 1'));
controls().children[2].listeners.click();
assert.ok(new URL(sendCart.href).searchParams.get('text').includes('MSM-001 | Quantity: 2'));
controls().children[1].value='4';controls().children[1].listeners.change();
assert.ok(new URL(sendCart.href).searchParams.get('text').includes('MSM-001 | Quantity: 4'));
controls().children[3].listeners.click();
assert.ok(!new URL(sendCart.href).searchParams.get('text').includes('Aashirvaad Atta'));
cartPanel.children[3].children[1].listeners.click();assert.equal(sendCart.hidden,true);assert.equal(sendCart.href,undefined);
const pure=mart.run('createGroceryCart('+JSON.stringify(m)+','+JSON.stringify(all)+')');
assert.equal(pure.url(),'');
for(const n of [-1,1.5,1000,NaN,Infinity])pure.set(0,n);
assert.equal(pure.entries().length,0);pure.set(0,999);assert.equal(pure.quantity(0),999);pure.set(0,0);assert.equal(pure.url(),'');
const nonGrocery=mart.run("createGroceryCart({businessType:'Ladies Wear',enquiryWhatsapp:'919921959748'},[{name:'Dress'}])");nonGrocery.set(0,1);assert.equal(nonGrocery.url(),'');
const missingNumber=mart.run("createGroceryCart({businessType:'Grocery'},[{name:'Rice'}])");missingNumber.set(0,1);assert.equal(missingNumber.url(),'');
console.log('PASS: Grocery-only cart, 50 add buttons, repeated adds, quantities, remove/clear, exact combined WhatsApp destination/message, invalid values and non-grocery exclusion.');

add(0);add(0);add(1);
assert.equal(cartPanel.children[5].textContent,'Grand total: ₹747.00');
assert.ok(new URL(sendCart.href).searchParams.get('text').includes('Unit MRP: ₹279.00 | Total: ₹558.00'));
assert.ok(new URL(sendCart.href).searchParams.get('text').includes('Grand total: ₹747.00'));
controls().children[1].value='4';controls().children[1].listeners.change();
assert.equal(cartPanel.children[5].textContent,'Grand total: ₹1,305.00');
controls().children[3].listeners.click();
assert.equal(cartPanel.children[5].textContent,'Grand total: ₹189.00');
assert.ok(groceryGrid.children.every(c=>c.children.some(x=>x.className==='product-price'&&x.textContent.includes('MRP ₹'))));
const fractional=mart.run("createGroceryCart({businessType:'Grocery',enquiryWhatsapp:'917666069942'},[{name:'Test',mrp:0.1},{name:'Other',mrp:0.2}])");
fractional.set(0,1);fractional.set(1,1);assert.equal(fractional.total(),30);
const unpriced=mart.run("createGroceryCart({businessType:'Grocery',enquiryWhatsapp:'917666069942'},[{name:'Unknown'}])");
unpriced.set(0,1);assert.equal(unpriced.total(),null);assert.ok(decodeURIComponent(unpriced.url()).includes('Awaiting prices'));
console.log('PASS: exact poster names/packs/MRPs, line totals, grand totals, quantity edits/removal, paise arithmetic and unknown prices.');
