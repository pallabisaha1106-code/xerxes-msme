
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
function setup(saved,blocked=false){
 const nodes=new Map(),get=id=>{if(!nodes.has(id))nodes.set(id,{value:'',innerHTML:'',children:[],listeners:{},append(n){this.children.push(n);},addEventListener(k,f){this.listeners[k]=f;},showModal(){this.open=true;},close(){this.open=false;this.listeners.close?.();},focus(){this.focused=true;}});return nodes.get(id);};
 let record=saved,selection;
 const ctx=vm.createContext({window:{},document:{querySelector:get,createElement:()=>({})},localStorage:{getItem(){if(blocked)throw Error('blocked');return record;},setItem(k,v){if(blocked)throw Error('blocked');record=v;}}});
 vm.runInContext(fs.readFileSync('dist/data/businesses.js','utf8'),ctx);
 vm.runInContext(fs.readFileSync('dist/js/discovery-welcome.js','utf8'),ctx);
 ctx.window.initializeDiscoveryWelcome({...ctx.window.marketplaceData,kind:'shops',onApply(s){selection=s;}});
 return {get,ctx,selection:()=>selection,record:()=>JSON.parse(record||'null')};
}
const app=setup(null);assert.equal(app.get('#discovery-welcome').open,true);
app.get('#welcome-area').value='Thergaon';app.get('#welcome-area').listeners.change();
assert.equal(app.get('#welcome-category').value,'All');
app.get('#welcome-category').value='Computer/Laptop shop';
app.get('#welcome-form').listeners.submit({preventDefault(){}});
assert.equal(app.get('#discovery-welcome').open,false);
assert.equal(app.selection().area,'Thergaon');assert.equal(app.selection().category,'Computer/Laptop shop');
assert.equal(app.record().seen,true);
const matches=app.ctx.window.marketplaceData.businesses.filter(b=>b.area===app.selection().area&&b.businessType===app.selection().category);
assert.equal(matches.length,1);assert.equal(matches[0].businessName,'Computer Town');
const returning=setup(JSON.stringify(app.record()));assert.notEqual(returning.get('#discovery-welcome').open,true);assert.equal(returning.selection().area,'Thergaon');
returning.ctx.window.saveDiscoverySelection({area:'',category:'All'});assert.equal(returning.record().category,'All');
const stale=setup(JSON.stringify({seen:true,area:'Removed area',category:'Removed category'}));assert.equal(stale.selection().area,'');assert.equal(stale.selection().category,'All');
const skipped=setup(null);skipped.get('#welcome-skip').listeners.click();assert.equal(skipped.record().seen,true);
assert.equal(setup('{bad').get('#discovery-welcome').open,true);
const blocked=setup(null,true);blocked.get('#welcome-form').listeners.submit({preventDefault(){}});assert.equal(blocked.get('#discovery-welcome').open,false);
const js=fs.readFileSync('dist/js/marketplace.js','utf8');assert.ok(js.includes('window.saveDiscoverySelection({area:'));
for(const f of ['index.html','wholesalers.html']){
 const html=fs.readFileSync('dist/'+f,'utf8');
 assert.ok(html.includes('id="discovery-welcome"'));assert.ok(html.includes('aria-labelledby="welcome-title"'));
 assert.ok(html.indexOf('src="js/discovery-welcome.js"')<html.indexOf('src="js/marketplace.js"'));
}
console.log('PASS: first visit, area/category filtering, saved preferences, returning visitor, reset persistence, stale values, skip, malformed/blocked storage and both page integrations.');

