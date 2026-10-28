const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const { Readable } = require('node:stream');
const { randomUUID } = require('node:crypto');
const {createCatalogueStore,receiveCatalogue}=require('./catalogue-store.cjs');
const catalogueStore=createCatalogueStore(path.join(__dirname,'submissions','catalogues.sqlite'));
const root = path.join(__dirname,'dist');
const submissionRoot = path.join(__dirname,'submissions');
const requiredFields = ['businessKind','businessName','businessCategory','phone','businessWhatsapp','ownerName','email','streetLandmark','area','pincode','city','state'];
const requiredFiles = ['shopPhoto','businessDocument','ownerDocument'];
const allowedTypes = new Map([['image/jpeg','.jpg'],['image/png','.png'],['image/webp','.webp'],['application/pdf','.pdf']]);

function json(res,status,body){res.writeHead(status,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store'});res.end(JSON.stringify(body));}
async function saveRegistration(req,res){
 const declaredLength=Number(req.headers['content-length']||0);
 if(declaredLength>27*1024*1024)return json(res,413,{error:'The uploaded files are too large.'});
 try{
  const request=new Request('http://localhost/api/registrations',{method:'POST',headers:req.headers,body:Readable.toWeb(req),duplex:'half'});
  const form=await request.formData();
  const fields={};
  for(const name of requiredFields){const value=String(form.get(name)||'').trim();if(!value)return json(res,400,{error:`Missing required field: ${name}`});fields[name]=value.slice(0,500);}
  if(!/^\d{6}$/.test(fields.pincode))return json(res,400,{error:'Pincode must contain exactly 6 digits.'});
  if(!/^\S+@\S+\.\S+$/.test(fields.email))return json(res,400,{error:'Please provide a valid email address.'});
  const files=[];
  for(const name of requiredFiles){
   const file=form.get(name);
   if(!file||typeof file.arrayBuffer!=='function'||!file.size)return json(res,400,{error:`Missing required file: ${name}`});
   if(file.size>8*1024*1024)return json(res,413,{error:`${name} is larger than 8 MB.`});
   const extension=allowedTypes.get(file.type);
   if(!extension||(name==='shopPhoto'&&file.type==='application/pdf'))return json(res,415,{error:`Unsupported file type for ${name}.`});
   files.push({name,file,extension});
  }
  const now=new Date();
  const reference=`NB-${now.toISOString().slice(0,10).replaceAll('-','')}-${randomUUID().slice(0,8).toUpperCase()}`;
  const directory=path.join(submissionRoot,reference);
  fs.mkdirSync(directory,{recursive:true});
  const savedFiles={};
  for(const item of files){const filename=item.name+item.extension;fs.writeFileSync(path.join(directory,filename),Buffer.from(await item.file.arrayBuffer()));savedFiles[item.name]=filename;}
  fs.writeFileSync(path.join(directory,'application.json'),JSON.stringify({reference,submittedAt:now.toISOString(),status:'pending-review',fields,files:savedFiles},null,2));
  return json(res,201,{ok:true,reference});
 }catch(error){console.error('Registration submission failed:',error);return json(res,500,{error:'The server could not save this application.'});}
}

http.createServer((req,res)=>{
 let url; try {url=decodeURIComponent(new URL(req.url,'http://localhost').pathname);} catch {res.writeHead(400).end();return;}
 if(req.method==='POST'&&url==='/api/grocery-catalogues'){receiveCatalogue(req,res,catalogueStore);return;}
 if(req.method==='POST'&&url==='/api/registrations'){saveRegistration(req,res);return;}
 if(req.method!=='GET'&&req.method!=='HEAD'){res.writeHead(405,{'Allow':'GET, HEAD, POST'}).end();return;}
 const file=path.resolve(root,'.'+(url==='/'?'/index.html':url));
 if(!file.startsWith(root+path.sep)){res.writeHead(403).end();return;}
 fs.readFile(file,(error,data)=>{if(error){res.writeHead(404).end('Not found');return;}res.setHeader('Content-Type',({'.html':'text/html; charset=utf-8','.css':'text/css','.js':'text/javascript','.svg':'image/svg+xml'})[path.extname(file)]||'application/octet-stream');if(req.method==='HEAD')res.end();else res.end(data);});
}).listen(4173,'127.0.0.1',()=>console.log('Local: http://127.0.0.1:4173'));
