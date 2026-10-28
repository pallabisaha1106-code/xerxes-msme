const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
function setup(saved,blocked=false,sessionVisited=false,navigationType='navigate'){
 const nodes=new Map(),get=id=>{if(!nodes.has(id))nodes.set(id,{value:'',innerHTML:'',children:[],listeners:{},validityMessage:'',append(n){this.children.push(n);},addEventListener(k,f){this.listeners[k]=f;},querySelectorAll(){return [];},setCustomValidity(v){this.validityMessage=v;},reportValidity(){this.reported=true;},showModal(){this.open=true;},close(){this.open=false;this.listeners.close?.();},focus(){this.focused=true;}});return nodes.get(id);};
 let record=saved,selection;
 let sessionRecord=sessionVisited?'true':null;
 const ctx=vm.createContext({window:{},document:{querySelector:get,createElement:()=>({})},performance:{getEntriesByType(){return [{type:navigationType}];}},sessionStorage:{getItem(){return sessionRecord;},setItem(k,v){sessionRecord=v;}},localStorage:{getItem(){if(blocked)throw Error('blocked');return record;},setItem(k,v){if(blocked)throw Error('blocked');record=v;}}});
 vm.runInContext(fs.readFileSync('dist/data/businesses.js','utf8'),ctx);
 vm.runInContext(fs.readFileSync('dist/js/discovery-welcome.js','utf8'),ctx);
 ctx.window.initializeDiscoveryWelcome({...ctx.window.marketplaceData,kind:'shops',welcomeCategories:ctx.window.marketplaceData.config.welcomeCategories,alwaysShow:ctx.window.marketplaceData.config.alwaysShowDiscovery,onApply(s){selection=s;}});
 return {get,ctx,selection:()=>selection,record:()=>JSON.parse(record||'null')};
}
const app=setup(null);assert.equal(app.get('#discovery-welcome').open,true);
assert.deepEqual(app.get('#welcome-category').children.map(node=>node.value),['All','Grocery','Restaurants','Ladies Wear','Beauty & Cosmetics']);
for(const city of ['Pune','Mumbai','Nagpur','Nashik'])assert.ok(app.get('#welcome-cities').innerHTML.includes('data-city="'+city+'"'));
app.get('#welcome-pincode').value='411045';app.get('#welcome-pincode').listeners.input();
app.get('#welcome-category').value='Grocery';app.get('#welcome-form').listeners.submit({preventDefault(){}});
assert.equal(app.get('#discovery-welcome').open,false);assert.equal(app.selection().pincode,'411045');assert.equal(app.selection().city,'');assert.equal(app.selection().category,'Grocery');assert.equal(app.record().seen,true);
const returning=setup(JSON.stringify(app.record()),false,true,'navigate');assert.notEqual(returning.get('#discovery-welcome').open,true);assert.equal(returning.selection().pincode,'411045');
returning.ctx.window.saveDiscoverySelection({pincode:'',city:'Pune',category:'All'});assert.equal(returning.record().city,'Pune');
const refreshed=setup(JSON.stringify(app.record()),false,true,'reload');assert.equal(refreshed.get('#discovery-welcome').open,true);assert.equal(refreshed.selection(),undefined);
const stale=setup(JSON.stringify({seen:true,pincode:'bad',city:'Delhi',category:'Removed'}),false,true,'navigate');assert.equal(stale.selection().pincode,'');assert.equal(stale.selection().city,'');assert.equal(stale.selection().category,'All');
const invalid=setup(null);invalid.get('#welcome-pincode').value='123';invalid.get('#welcome-form').listeners.submit({preventDefault(){}});assert.equal(invalid.get('#discovery-welcome').open,true);assert.equal(invalid.get('#welcome-pincode').reported,true);
const skipped=setup(null);skipped.get('#welcome-skip').listeners.click();assert.equal(skipped.record().seen,true);
assert.equal(setup('{bad').get('#discovery-welcome').open,true);
const blocked=setup(null,true);blocked.get('#welcome-form').listeners.submit({preventDefault(){}});assert.equal(blocked.get('#discovery-welcome').open,false);
const js=fs.readFileSync('dist/js/marketplace.js','utf8');assert.ok(js.includes('welcomeCategories:config.welcomeCategories'));assert.ok(js.includes('alwaysShow:config.alwaysShowDiscovery'));assert.ok(js.includes('activeCity'));
for(const f of ['index.html','wholesalers.html']){const html=fs.readFileSync('dist/'+f,'utf8');assert.ok(html.includes('id="welcome-pincode"'));assert.ok(html.includes('id="welcome-cities"'));assert.ok(html.indexOf('src="js/discovery-welcome.js"')<html.indexOf('src="js/marketplace.js"'));}
console.log('PASS: popup on first home entry and browser refresh, no popup on internal navigation, pincode, cities, categories, saved preferences and both page integrations.');
