const {DatabaseSync}=require('node:sqlite');
const {randomUUID}=require('node:crypto');
const fs=require('node:fs'),path=require('node:path');
const sharedImages=new Map(JSON.parse(fs.readFileSync(path.join(__dirname,'dist/data/grocery-product-images.json'),'utf8')).map(p=>[p.id,p.image]));
function createCatalogueStore(filename){
 fs.mkdirSync(path.dirname(filename),{recursive:true});
 const db=new DatabaseSync(filename);
 db.exec('PRAGMA foreign_keys=ON; CREATE TABLE IF NOT EXISTS catalogues (id TEXT PRIMARY KEY, business_name TEXT NOT NULL, submitted_at TEXT NOT NULL); CREATE TABLE IF NOT EXISTS catalogue_products (catalogue_id TEXT NOT NULL REFERENCES catalogues(id), product_id TEXT NOT NULL, name TEXT NOT NULL, category TEXT NOT NULL, price_paise INTEGER NOT NULL, pack_quantity TEXT NOT NULL, image TEXT NOT NULL, PRIMARY KEY(catalogue_id,product_id));');
 function save(data){
  const validText=(v,max)=>typeof v==='string'&&v.trim().length>0&&v.length<=max;
  if(!data||data.businessType!=='Grocery'||!validText(data.businessName,120)||!Array.isArray(data.products)||!data.products.length||data.products.length>500)throw new Error('Provide a grocery shop name and 1–500 products.');
  const ids=new Set();
  for(const p of data.products){
   if(!p||!validText(p.id,100)||ids.has(p.id)||!validText(p.name,120)||!validText(p.category,120)||!validText(p.packSize,60)||typeof p.mrp!=='number'||!Number.isFinite(p.mrp)||p.mrp<=0||p.mrp>9999999.99||Math.abs(p.mrp*100-Math.round(p.mrp*100))>0.00001)throw new Error('Every product needs a unique reference, name, category, valid price and pack quantity.');
   if(typeof p.image!=='string'||p.image.length>3*1024*1024||(p.image&&p.image!==sharedImages.get(p.id)&&!/^data:image\/jpeg;base64,[A-Za-z0-9+/]+={0,2}$/.test(p.image)))throw new Error('Invalid product photo.');
   ids.add(p.id);
  }
  const id=randomUUID();db.exec('BEGIN');
  try{
   db.prepare('INSERT INTO catalogues VALUES (?,?,?)').run(id,data.businessName.trim(),new Date().toISOString());
   const insert=db.prepare('INSERT INTO catalogue_products VALUES (?,?,?,?,?,?,?)');
   for(const p of data.products)insert.run(id,p.id,p.name.trim(),p.category.trim(),Math.round(p.mrp*100),p.packSize.trim(),p.image);
   db.exec('COMMIT');return id;
  }catch(error){db.exec('ROLLBACK');throw error;}
 }
 return {save,close:()=>db.close()};
}
async function receiveCatalogue(req,res,store){
 const reply=(status,body)=>{res.writeHead(status,{'Content-Type':'application/json','Cache-Control':'no-store'});res.end(JSON.stringify(body));};
 if(!(req.headers['content-type']||'').startsWith('application/json'))return reply(415,{error:'Send the catalogue as JSON.'});
 try{
  let size=0;const chunks=[];
  for await(const chunk of req){size+=chunk.length;if(size>20*1024*1024)return reply(413,{error:'Catalogue exceeds 20 MB. Use fewer or smaller photos.'});chunks.push(chunk);}
  let data;try{data=JSON.parse(Buffer.concat(chunks).toString('utf8'));}catch{return reply(400,{error:'Invalid catalogue JSON.'});}
  try{const reference=store.save(data);return reply(201,{ok:true,reference});}catch(error){if(error.code){console.error(error);return reply(500,{error:'The database could not save your catalogue. Please retry.'});}return reply(400,{error:error.message});}
 }catch{return reply(500,{error:'Unable to receive the catalogue. Please retry.'});}
}
module.exports={createCatalogueStore,receiveCatalogue};
