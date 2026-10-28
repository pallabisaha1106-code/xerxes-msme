const fs=require('node:fs'), vm=require('node:vm'), assert=require('node:assert/strict'), path=require('node:path');
const nodes=new Map();
function node(key){if(!nodes.has(key)) nodes.set(key,{value:'',innerHTML:'',textContent:'',hidden:false,dataset:{},attributes:{},listeners:{},classList:{add(){},remove(){},toggle(){return true;}},addEventListener(type,fn){this.listeners[type]=fn;},append(){},setAttribute(k,v){this.attributes[k]=v;},querySelector(){return node(key+' child');},querySelectorAll(){return [];},showModal(){this.open=true;},close(){this.open=false;},scrollIntoView(){}});return nodes.get(key);}
const context=vm.createContext({window:{},document:{querySelector:node,querySelectorAll(){return [];},createElement(){return {value:'',textContent:''};},addEventListener(){}},location:{href:'http://localhost:4173/'},URL,matchMedia(){return {matches:true};}});
vm.runInContext(fs.readFileSync('dist/data/businesses.js','utf8'),context);
vm.runInContext(fs.readFileSync('dist/js/marketplace.js','utf8'),context);
const run=code=>vm.runInContext(code,context);
assert.equal(run('businesses.length'),14);
assert.equal(new Set(run('businesses.map(b=>b.id)')).size,14);
for(const business of context.window.marketplaceData.businesses){assert.ok(context.window.marketplaceData.categories.some(c=>c.name===business.businessType));assert.ok(/^\d{6}$/.test(business.pincode));assert.ok(business.products.length>=2);for(const image of [business.shopPhoto,...business.products.map(p=>p.image)])assert.ok(fs.existsSync(path.join('dist',image)),image);assert.ok(['www.google.com','maps.app.goo.gl','share.google'].includes(new URL(business.links.googleMaps).hostname));}
for(const [query,count] of [['Veera',1],['Ladies Wear',1],['ceramic',1],['411045',2],['Aundh',2],['cotton',2],['no-such-shop',0]]){node('#search').value=query;run('render()');assert.equal(run('businesses.filter(matches).length'),count,query);}
run('reset()'); assert.equal(run('businesses.filter(matches).length'),14);
node('#area').value='Baner';run("activeCategory='Jewellery'");assert.equal(run('businesses.filter(matches).length'),1);
node('#pincode').value='411007';run('render()');assert.equal(node('#empty').hidden,false);
run('reset()');assert.equal(node('#empty').hidden,true);assert.equal(node('#area').value,'');
assert.equal(run("whatsappURL('', 'hello')"),'');assert.equal(run("whatsappURL('919876543210','a & b')"),'https://wa.me/919876543210?text=a%20%26%20b');assert.equal(run("safeURL('javascript:alert(1)')"),'');
node('#business-grid').listeners.click({target:{closest(){return {dataset:{business:'veera',product:'0'}};}}});assert.equal(node('#lightbox').open,true);assert.equal(node('#preview-title').textContent,'Anarkali Dresses');
node('.menu-button').listeners.click();assert.equal(node('.menu-button').attributes['aria-expanded'],true);
const html=fs.readFileSync('dist/index.html','utf8');for(const [,file] of html.matchAll(/(?:src|href)="((?:css|js|data|images)\/[^\"]+)"/g))assert.ok(fs.existsSync(path.join('dist',file)),file);
assert.ok(html.includes('Left Side-Rail Ad')&&html.includes('Right Side-Rail Ad'));
const shopAds = [...html.matchAll(/<a class="shop-ad"[^>]*>/g)].map(match => match[0]);
assert.equal(shopAds.length,2);
assert.ok(shopAds[0].includes('href="https://www.veeracollections.com/"'));
assert.ok(shopAds[1].includes('href="https://www.computertownstore.com/"'));
for(const ad of shopAds){assert.ok(ad.includes('target="_blank"'));assert.ok(ad.includes('rel="noopener noreferrer"'));}
console.log('PASS: 14 listings; unique IDs; assets; categories; 7 keyword searches; combined area/category/pincode filters; empty/reset states; WhatsApp URL encoding; unsafe URL rejection; product dialog; mobile menu; static page references; both ad rails.');


// Contact rows: exact destinations, optional fields, accessibility and injection rejection.
const expectedLinks = {
instagram:'https://www.instagram.com/veera.collectionss',
facebook:'https://www.facebook.com/share/18ES1ZSB3Z/',
youtube:'https://www.youtube.com/@veeracollectionss',
googleMaps:'https://maps.app.goo.gl/ducwauewX8tGmd2Z6',
whatsapp:'https://chat.whatsapp.com/IpsTwctzlrhH2lqOZne2RV?s=cl&p=i&ilr=0&amv=0',
website:'https://www.veeracollections.com/'
};
assert.deepEqual(JSON.parse(run('JSON.stringify(businesses.find(b=>b.id==="veera").links)')),expectedLinks);
const decode=s=>s.replace(/&amp;/g,'&').replace(/&quot;/g,'"').replace(/&#39;/g,"'").replace(/&lt;/g,'<').replace(/&gt;/g,'>');
const row=run('renderContactLinks(businesses.find(b=>b.id==="veera"))');
assert.deepEqual([...row.matchAll(/href="([^"]+)"/g)].map(m=>decode(m[1])),Object.values(expectedLinks));
for(const attr of ['target="_blank"','rel="noopener noreferrer"','aria-label="','title="','aria-hidden="true"','focusable="false"'])assert.equal(row.split(attr).length-1,6,attr);
assert.ok(!row.includes('demo-contact')&&!row.includes('Chat on WhatsApp'));
assert.equal(run('renderContactLinks(businesses.find(b=>b.id==="daily"))').match(/class="contact-link"/g).length,1);
for(const candidate of [{},{links:{}},{links:null},{links:{instagram:'',website:null}},{links:{whatsapp:'javascript:alert(1)',website:'file:///tmp/a'}}]){
context.candidate=candidate;assert.equal(run('renderContactLinks(candidate)'),'');
}
for(const value of ['',null,undefined,42,' ','not-a-url','/relative','//example.com','javascript:alert(1)','data:text/html,hi','file:///tmp/a','ftp://example.com','https://','https://bad host.test','https://example.com\n','https:\\\\example.com','https://user:pass@example.com']){
context.badURL=value;assert.equal(run('contactURL(badURL)'),'');
}
context.candidate={businessName:'Shop"><script>alert(1)</script>',links:{website:'https://example.com/?q="&x=2',instagram:'javascript:alert(1)'}};
const escapedRow=run('renderContactLinks(candidate)');assert.equal(escapedRow.match(/class="contact-link"/g).length,1);assert.ok(!escapedRow.includes('<script>'));
assert.equal(decode(escapedRow.match(/href="([^"]+)"/)[1]),context.candidate.links.website);
const css=fs.readFileSync('dist/css/style.css','utf8');
assert.match(css,/\.card-actions\{[^}]*flex-wrap:nowrap/);
assert.match(css,/\.contact-link\{[^}]*min-width:0;min-height:30px/);
assert.ok(css.includes('a:focus-visible'));
assert.ok(html.includes('href="register.html?type=retailer"'));
const generatedFiles=new Map();
vm.runInNewContext(fs.readFileSync('generate-assets.cjs','utf8'),{
require(name){if(name==='node:fs')return {writeFileSync(file,value){generatedFiles.set(path.normalize(file),value);}};if(name==='node:path')return path;throw Error(name);},
__dirname:process.cwd(),console:{log(){}}
});
const generated={window:{}};
vm.runInNewContext(generatedFiles.get(path.join(process.cwd(),'dist','data','businesses.js')),generated);
const liveData=context.window.marketplaceData.businesses;
for(const b of generated.window.marketplaceData.businesses){
assert.deepEqual(JSON.parse(JSON.stringify(b.links)),JSON.parse(JSON.stringify(liveData.find(item=>item.id===b.id).links)));
assert.ok(!('whatsapp' in b)&&!('googleMapsUrl' in b));
}
console.log('PASS: exact six Veera URLs; partial/absent/invalid rows; HTTP(S) validation; safe escaping; accessible labels and tooltips; 30px single-row CSS; sample generator schema and link parity.');


node("#search").value="Computer Town";run("render()");assert.equal(run("businesses.filter(matches).length"),1);
node("#search").value="Thergaon";run("render()");assert.equal(run("businesses.filter(matches).length"),1);
run("reset(); activeCategory = \"Computer/Laptop shop\"; render()");assert.equal(run("businesses.filter(matches).length"),1);
assert.equal(run("renderContactLinks(businesses.find(b=>b.id===\"computer-town\"))").match(/class="contact-link"/g).length,4);
run("reset()");

for(const term of ['Krishna','Aashirvaad Atta','Tata Salt','Rice','Toor Dal','Surf Excel Detergent','Poonawale','411033','Grocery']){run('reset()');node('#search').value=term;run('render()');assert.ok(node('#business-grid').innerHTML.includes('data-shop="mateshwari-super-mart"'),term);}

assert.deepEqual(JSON.parse(JSON.stringify(generated.window.marketplaceData.businesses.find(b=>b.id==="mateshwari-super-mart"))),JSON.parse(JSON.stringify(liveData.find(b=>b.id==="mateshwari-super-mart"))));
