const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const {DatabaseSync}=require('node:sqlite');const {randomBytes,createHash,timingSafeEqual}=require('node:crypto');
function createRetailerStore(filename,source){
 fs.mkdirSync(path.dirname(filename),{recursive:true});const db=new DatabaseSync(filename);
 db.exec('CREATE TABLE IF NOT EXISTS retailer_updates (id TEXT PRIMARY KEY, data TEXT NOT NULL, revision INTEGER NOT NULL DEFAULT 0, key_hash TEXT);');
 const context={window:{}};vm.runInNewContext(fs.readFileSync(source,'utf8'),context);const original=context.window.marketplaceData;
 for(const shop of original.businesses){[...shop.products,...(shop.productPage?.products||[])].forEach((p,i)=>{p.id=p.id||shop.id+'-'+(i+1);});}
 for(const shop of original.businesses)db.prepare('INSERT OR IGNORE INTO retailer_updates(id,data) VALUES (?,?)').run(shop.id,JSON.stringify(shop));
 const hash=key=>createHash('sha256').update(key).digest('hex');
 function get(id){const row=db.prepare('SELECT * FROM retailer_updates WHERE id=?').get(id);if(!row)throw Error('Retailer not found.');return row;}
 function authorize(id,key){const row=get(id);if(!row.key_hash||!key||!timingSafeEqual(Buffer.from(row.key_hash,'hex'),Buffer.from(hash(key),'hex')))throw Error('Invalid retailer access key.');return row;}
 function issueKey(id){get(id);const key=randomBytes(24).toString('hex');db.prepare('UPDATE retailer_updates SET key_hash=? WHERE id=?').run(hash(key),id);return key;}
 function validate(shop){
 const text=(v,n)=>typeof v==='string'&&v.trim().length>0&&v.length<=n;
 for(const k of ['businessName','businessType','address','area','pincode'])if(!text(shop[k],k==='address'?500:120))throw Error('Complete all required retailer details.');
 if(!/^\d{6}$/.test(shop.pincode))throw Error('Enter a six-digit pincode.');
 if(shop.enquiryWhatsapp&&!/^[1-9]\d{7,14}$/.test(shop.enquiryWhatsapp))throw Error('WhatsApp number must include country code and digits only.');
 for(const value of Object.values(shop.links||{})){if(!value)continue;try{if(!['https:','http:'].includes(new URL(value).protocol))throw 0;}catch{throw Error('Contact links must be full HTTP or HTTPS URLs.');}}
 const all=[...shop.products,...(shop.productPage?.products||[])];if(all.length>500)throw Error('Limit the catalogue to 500 products.');const ids=new Set();
 for(const p of all){if(!text(p.name,120)||!text(p.id,120)||ids.has(p.id))throw Error('Products need unique references and names.');ids.add(p.id);
 if(p.mrp!==undefined&&(!Number.isFinite(p.mrp)||p.mrp<0||p.mrp>9999999.99))throw Error('Enter a valid product price.');
 if(p.packSize!==undefined&&(typeof p.packSize!=='string'||p.packSize.length>120))throw Error('Invalid pack quantity.');
 if(typeof p.image!=='string'||p.image.length>3000000||!(/^images\/[a-zA-Z0-9_ ./-]+$/.test(p.image)&&!p.image.includes('..')||/^data:image\/jpeg;base64,[A-Za-z0-9+/]+={0,2}$/.test(p.image)))throw Error('Each product requires a valid image.');
 }
 }
 function read(id,key){const row=authorize(id,key);return {shop:JSON.parse(row.data),revision:row.revision};}
 function save(id,key,payload){const row=authorize(id,key);if(payload.revision!==row.revision)throw Error('This shop was updated elsewhere. Reload before saving.');const shop=JSON.parse(row.data);
 if(payload.section==='details')for(const k of ['businessName','businessType','address','area','city','pincode','enquiryWhatsapp','links']){if(payload.details[k]!==undefined)shop[k]=payload.details[k];}
 else if(payload.section==='catalogue'){
 if(!Array.isArray(payload.products))throw Error('Invalid products.');
 shop.products=payload.products.slice(0,3);if(shop.productPage)shop.productPage.products=payload.products.slice(3);else if(payload.products.length>3)shop.productPage={enabled:false,url:'',products:payload.products.slice(3)};
 }else throw Error('Invalid update section.');
 validate(shop);db.prepare('UPDATE retailer_updates SET data=?,revision=revision+1 WHERE id=?').run(JSON.stringify(shop),id);return {shop,revision:row.revision+1};
 }
 return {read,save,issueKey,publicData(){return {...original,businesses:db.prepare('SELECT data FROM retailer_updates ORDER BY rowid').all().map(row=>JSON.parse(row.data))};},close:()=>db.close()};
}
async function retailerAPI(req,res,store,id){
 const reply=(code,data)=>{res.writeHead(code,{'Content-Type':'application/json','Cache-Control':'no-store'});res.end(JSON.stringify(data));};
 const key=(req.headers.authorization||'').replace(/^Bearer /,'');
 try{if(req.method==='GET')return reply(200,store.read(id,key));if(req.method!=='PUT')return reply(405,{error:'Method not allowed.'});
 store.read(id,key);let size=0,chunks=[];for await(const chunk of req){size+=chunk.length;if(size>20*1024*1024)return reply(413,{error:'Catalogue exceeds 20 MB.'});chunks.push(chunk);}
 return reply(200,store.save(id,key,JSON.parse(Buffer.concat(chunks).toString('utf8'))));
 }catch(error){return reply(error.message.includes('access key')?403:400,{error:error.message});}
}
module.exports={createRetailerStore,retailerAPI};
