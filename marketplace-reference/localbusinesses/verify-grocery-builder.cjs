const fs=require('fs'),vm=require('vm'),assert=require('assert/strict');
const source=fs.readFileSync('dist/js/grocery-builder.js','utf8');
const core={};vm.createContext(core);vm.runInContext(source.split('(() => {')[0],core);
for(const bad of ['', '-1','0','NaN','Infinity','1.234','1e3','₹20'])assert.equal(core.groceryPrice(bad),null);
assert.equal(core.groceryPrice('125.50'),125.5);
assert.throws(()=>core.groceryExport('',[]));assert.throws(()=>core.groceryExport('Shop',[]));
const result=core.groceryExport('Shop',[{id:'1',name:'Milk',category:'Milk & Dairy Products',price:'60',quantity:'1 L',photo:'',custom:false}]);
assert.equal(result.businessType,'Grocery');assert.equal(result.products[0].mrp,60);assert.equal(result.products[0].packSize,'1 L');
assert.throws(()=>core.groceryExport('Shop',[{name:'Milk',category:'Dairy',price:'abc',quantity:'1 L'}]));
class El{
 constructor(){this.children=[];this.events={};this.value='';this.files=[];}
 append(...x){this.children.push(...x);}replaceChildren(...x){this.children=x;}
 setAttribute(){}addEventListener(n,f){this.events[n]=f;}querySelector(){return new El();}
}
const nodes=new Map(),get=s=>{if(!nodes.has(s))nodes.set(s,new El());return nodes.get(s)};
get('#custom-product').elements={category:new El()};
let saved;
const context={window:{},document:{querySelector:get,createElement:()=>new El()},localStorage:{getItem:()=>null,setItem:(k,v)=>saved=v}};
vm.createContext(context);vm.runInContext(fs.readFileSync('dist/data/grocery-defaults.js','utf8'),context);vm.runInContext(source,context);
assert.equal(Object.keys(context.window.groceryDefaults).length,18);
const groups=get('#default-products');assert.equal(groups.children.length,18);
const milk=groups.children[0].children[1].children[0];milk.checked=true;milk.events.change();
assert.equal(get('#selected-count').textContent,1);
const fields=get('#selected-products').children[0].children.slice(1,3);
fields[0].children[0].value='60';fields[0].children[0].events.input();
fields[1].children[0].value='1 L';fields[1].children[0].events.input();
get('#owner-shop').value='Grocery Store';get('#save-catalogue').events.click();
assert.equal(JSON.parse(saved).products[0].price,'60');assert.equal(JSON.parse(saved).products[0].quantity,'1 L');
get('#product-search').value='nonexistent';get('#product-search').events.input();assert.equal(get('#default-count').textContent,'0 products available');
get('#product-search').value='';get('#category-filter').value='Hair Care';get('#category-filter').events.change();assert.equal(groups.children.length,1);
get('#selected-products').children[0].children[3].children[0].events.click();assert.equal(get('#selected-count').textContent,0);
assert.ok(fs.existsSync('dist/images/grocery-product-reference.png'));
assert.ok(fs.readFileSync('dist/index.html','utf8').includes('href="grocery-catalogue.html"'));
console.log('PASS: 18 categories, default selection/removal, search/category filters, editable price/quantity, local draft, export schema, invalid prices and required fields, reference image and entry link.');


const manifest=JSON.parse(fs.readFileSync('dist/data/grocery-product-images.json','utf8'));for(const p of manifest)assert.ok(fs.existsSync('dist/'+p.image));assert.equal(JSON.parse(saved).products[0].photo,'images/grocery-products/groc-1-1.jpg');console.log('PASS: '+manifest.length+' reusable image assets and selected product photo retention.');
