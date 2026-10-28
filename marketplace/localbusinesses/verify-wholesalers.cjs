const fs=require('node:fs'), vm=require('node:vm'), assert=require('node:assert/strict'), path=require('node:path');
const nodes=new Map();
function node(key){if(!nodes.has(key)) nodes.set(key,{value:'',innerHTML:'',textContent:'',hidden:false,dataset:{},attributes:{},listeners:{},classList:{add(){},remove(){},toggle(){return true;}},addEventListener(type,fn){this.listeners[type]=fn;},append(){},setAttribute(k,v){this.attributes[k]=v;},querySelector(){return node(key+' child');},querySelectorAll(){return [];},showModal(){this.open=true;},close(){this.open=false;},scrollIntoView(){}});return nodes.get(key);}
const context=vm.createContext({window:{},document:{querySelector:node,querySelectorAll(){return [];},createElement(){return {value:'',textContent:''};},addEventListener(){}},location:{href:'http://localhost:4173/'},URL,matchMedia(){return {matches:true};}});
vm.runInContext(fs.readFileSync('dist/data/wholesalers.js','utf8'),context);
vm.runInContext(fs.readFileSync('dist/js/marketplace.js','utf8'),context);
const run=code=>vm.runInContext(code,context);

assert.equal(run('businesses.length'),8);
assert.equal(new Set(run('businesses.map(b=>b.id)')).size,8);
assert.ok(node('#result-count').textContent.includes('8 wholesalers'));
assert.ok(node('#category-list').innerHTML.includes('All wholesalers'));
for(const [query,count] of [['textiles',1],['Market Yard',1],['411002',4],['Rice sacks',1],['missing supplier',0]]){
 node('#search').value=query;run('render()');assert.equal(run('businesses.filter(matches).length'),count,query);
}
run('reset()');node('#area').value='Raviwar Peth';run("activeCategory='Kids Wear'; render()");assert.equal(run('businesses.filter(matches).length'),1);
node('#pincode').value='411037';run('render()');assert.equal(node('#empty').hidden,false);
run('reset()');assert.equal(run('businesses.filter(matches).length'),8);
node('#business-grid').listeners.click({target:{closest(){return {dataset:{business:'wholesale-linen',product:'0'}};}}});
assert.equal(node('#lightbox').open,true);assert.equal(node('#preview-title').textContent,'Cotton fabric rolls');
assert.ok(run("config.submissionMessage.includes('wholesale business')"));
assert.equal(run("whatsappURL('',config.submissionMessage)"),'');
assert.ok(run("whatsappURL('919876543210',config.submissionMessage)").includes('wholesale%20business'));
for(const b of context.window.marketplaceData.businesses){
 assert.equal(run('renderContactLinks('+JSON.stringify(b)+')'),'');
 for(const image of [b.shopPhoto,...b.products.map(p=>p.image)])assert.ok(fs.existsSync(path.join('dist',image)),image);
 assert.ok(context.window.marketplaceData.categories.some(c=>c.name===b.businessType));
}
const wholesale=fs.readFileSync('dist/wholesalers.html','utf8'),retail=fs.readFileSync('dist/index.html','utf8');
for(const html of [wholesale,retail]){
 for(const [,ref] of html.matchAll(/(?:src|href)="((?:css|js|data|images)\/[^"]+|(?:index|wholesalers)\.html)"/g))assert.ok(fs.existsSync(path.join('dist',ref)),ref);
 for(const id of ['search','area','pincode','reset','business-grid','category-list'])assert.equal(html.split('id="'+id+'"').length-1,1);
 assert.ok(html.includes('href="wholesalers.html"'));assert.ok(html.includes('href="index.html"'));
}
assert.ok(wholesale.includes('aria-current="page" href="wholesalers.html"'));
assert.ok(wholesale.includes('src="data/wholesalers.js"'));
assert.ok(!wholesale.includes('src="data/businesses.js"'));
assert.ok(wholesale.includes('Sample wholesale businesses'));
assert.ok(wholesale.includes('List Your Wholesale Business on WhatsApp'));
console.log('PASS: independent wholesaler data; search and combined filters; reset/empty state; product preview; correct labels; submission message; optional contacts; links and assets on both pages.');
