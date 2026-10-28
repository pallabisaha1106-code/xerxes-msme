(()=>{
const $=s=>document.querySelector(s),make=(tag,text)=>{const e=document.createElement(tag);if(text)e.textContent=text;return e;};
let current=null,products=[],shopId='',busy=false,dirty=false,detailDirty=false,catalogueDirty=false;
let membership='Free';
const plans={Free:{limit:0,description:'Free includes your retailer details. A separate product catalogue page is not available. Select Plus or Premium to add a catalogue.'},Plus:{limit:100,description:'Plus includes a product catalogue with up to 100 products. You can add, edit and remove products within this limit.'},Premium:{limit:500,description:'Premium includes a product catalogue with up to 500 products. You can add, edit and remove products within this limit.'}};
const membershipBoxes=[...document.querySelectorAll('input[name="membership"]')];
function updateMembership(){
 const limit=plans[membership].limit;
 membershipBoxes.forEach(box=>box.checked=box.value===membership);
 $('#membership-tip').textContent=plans[membership].description;
 $('#products').hidden=!limit;
 $('#add-product').disabled=!limit||products.length>=limit;
 $('#save-products').disabled=busy||!limit||products.length>limit;
 $('#membership-limit').textContent=!limit?'Catalogue unavailable on Free. Choose Plus or Premium in Retailer details.':products.length+' / '+limit+' products'+(products.length>limit?' — remove '+(products.length-limit)+' products to preview this plan. Your products have been kept.':'');
}
membershipBoxes.forEach(box=>box.addEventListener('change',()=>{membership=box.value;updateMembership();}));
const say=text=>$('#status').textContent=text;
for(const shop of window.marketplaceData.businesses){const option=make('option',shop.businessName);option.value=shop.id;$('#retailer').append(option);}
const fields=['businessName','businessType','address','area','city','pincode','enquiryWhatsapp'];
const titles=['Business name','Business type / category','Address','Area','City','Pincode','WhatsApp number (country code + digits)'];
const links=['instagram','facebook','youtube','googleMaps','whatsapp','website'];
function input(parent,title,value,type='text'){const label=make('label',title),el=make('input');el.type=type;el.value=value??'';label.append(el);parent.append(label);return el;}
let detailInputs={},linkInputs={};
function renderDetails(){const root=$('#detail-fields');root.replaceChildren();fields.forEach((key,i)=>{detailInputs[key]=input(root,titles[i],current.shop[key]);detailInputs[key].maxLength=key==='address'?500:120;});links.forEach(key=>{linkInputs[key]=input(root,key+' URL',current.shop.links?.[key],'url');});}
async function photo(file){if(!file||!['image/jpeg','image/png','image/webp'].includes(file.type)||file.size>5*1024*1024)throw Error('Choose a JPG, PNG or WebP photo under 5 MB.');const image=await createImageBitmap(file),scale=Math.min(1,900/Math.max(image.width,image.height));const canvas=make('canvas');canvas.width=Math.round(image.width*scale);canvas.height=Math.round(image.height*scale);const context=canvas.getContext('2d');context.fillStyle='white';context.fillRect(0,0,canvas.width,canvas.height);context.drawImage(image,0,0,canvas.width,canvas.height);image.close();return canvas.toDataURL('image/jpeg',.85);}
function renderProducts(){updateMembership();const root=$('#products');root.replaceChildren();if(!products.length)root.append(make('p','No products yet. Add your first product below.'));
products.forEach(p=>{const row=make('article');row.className='edit-product';const img=make('img');img.src=p.image||'images/placeholder.svg';img.alt=p.name||'New product';row.append(img);
const group=make('div');group.className='product-fields';for(const [key,title] of [['name','Product name'],['category','Category'],['mrp','Price / MRP (₹)'],['packSize','Pack quantity / size']]){const el=input(group,title,p[key]);el.maxLength=key==='mrp'?10:120;el.addEventListener('input',()=>{if(key==='mrp'){if(el.value.trim()==='')delete p.mrp;else p.mrp=Number(el.value);}else p[key]=el.value;dirty=true;catalogueDirty=true;});}
const upload=input(group,'Product photo','','file');upload.accept='image/jpeg,image/png,image/webp';upload.addEventListener('change',async()=>{if(busy)return;busy=true;$('#save-products').disabled=true;try{p.image=await photo(upload.files[0]);img.src=p.image;dirty=true;catalogueDirty=true;say('Photo added to this preview only.');}catch(error){say(error.message);}finally{busy=false;updateMembership();}});
const remove=make('button','Delete product');remove.type='button';remove.addEventListener('click',()=>{products=products.filter(item=>item!==p);dirty=true;catalogueDirty=true;renderProducts();});row.append(group,remove);root.append(row);});}
function loadPreview(){membership='Free';shopId=$('#retailer').value;const shop=window.marketplaceData.businesses.find(b=>b.id===shopId);if(!shop)return;current={shop:JSON.parse(JSON.stringify(shop))};products=[...current.shop.products,...(current.shop.productPage?.products||[])].map((p,i)=>({...p,id:p.id||shopId+'-'+(i+1)}));renderDetails();renderProducts();$('#workspace').hidden=false;say('Previewing '+shop.businessName+'. Changes reset when you reload or choose another retailer.');}
$('#unlock').addEventListener('submit',event=>{event.preventDefault();loadPreview();});
function save(payload){if(payload.section==='catalogue'&&(!plans[membership].limit||products.length>plans[membership].limit)){say('Choose a plan that supports this catalogue size.');return;}if(payload.section==='details')Object.assign(current.shop,payload.details,{membership});say('Preview updated for this page only. Nothing has been saved or submitted.');}
$('#details-form').addEventListener('submit',event=>{event.preventDefault();const details=Object.fromEntries(fields.map(key=>[key,detailInputs[key].value.trim()]));details.links=Object.fromEntries(links.map(key=>[key,linkInputs[key].value.trim()]));save({section:'details',details});});
$('#add-product').addEventListener('click',()=>{if(!plans[membership].limit||products.length>=plans[membership].limit)return;products.push({id:'PRODUCT-'+crypto.randomUUID(),name:'',category:current.shop.businessType,packSize:'',image:''});dirty=true;catalogueDirty=true;renderProducts();});
$('#save-products').addEventListener('click',()=>save({section:'catalogue',products}));
loadPreview();
const menu=$('.menu-button');menu?.addEventListener('click',()=>menu.setAttribute('aria-expanded',$('#navigation').classList.toggle('open')));
})();
