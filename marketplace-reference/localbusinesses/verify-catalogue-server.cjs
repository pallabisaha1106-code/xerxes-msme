const assert=require('node:assert/strict'),fs=require('node:fs'),os=require('node:os'),path=require('node:path'),http=require('node:http');
const {createCatalogueStore,receiveCatalogue}=require('./catalogue-store.cjs');
const {DatabaseSync}=require('node:sqlite');
(async()=>{
const dir=fs.mkdtempSync(path.join(os.tmpdir(),'grocery-test-')),file=path.join(dir,'catalogue.sqlite');const store=createCatalogueStore(file);
const server=http.createServer((req,res)=>receiveCatalogue(req,res,store));await new Promise(r=>server.listen(0,'127.0.0.1',r));
try{
const url='http://127.0.0.1:'+server.address().port;
const data={businessType:'Grocery',businessName:'Test Grocery',products:[{id:'GROC-1-1',name:'Amul Milk',category:'Dairy',mrp:60.25,packSize:'1 L',image:'images/grocery-products/groc-1-1.jpg'}]};
const post=d=>fetch(url,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(d)});
let res=await post(data);assert.equal(res.status,201);const body=await res.json();assert.ok(body.reference);
assert.equal((await post({...data,businessType:'Clothing'})).status,400);
assert.equal((await post({...data,products:[{...data.products[0],mrp:-1}]})).status,400);
assert.equal((await post({...data,products:[data.products[0],data.products[0]]})).status,400);
assert.equal((await post({...data,products:[{...data.products[0],image:'images/grocery-products/groc-1-2.jpg'}]})).status,400);
store.close();const db=new DatabaseSync(file);assert.equal(db.prepare('SELECT COUNT(*) AS n FROM catalogues').get().n,1);const row=db.prepare('SELECT * FROM catalogue_products').get();assert.equal(row.price_paise,6025);assert.equal(row.pack_quantity,'1 L');assert.equal(row.image,'images/grocery-products/groc-1-1.jpg');db.close();
console.log('PASS: HTTP submission, reference, persisted SQLite data after reopen, integer prices, invalid category/price/duplicate rejection with no partial saves.');
}finally{await new Promise(r=>server.close(r));fs.rmSync(dir,{recursive:true,force:true});}
})();
