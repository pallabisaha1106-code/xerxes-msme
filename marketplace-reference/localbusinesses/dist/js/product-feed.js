(function(){
'use strict';
const $=s=>document.querySelector(s);
const shops=(window.marketplaceData?.businesses||[]).filter(shop=>shop.productPage?.enabled===true&&/^[a-zA-Z0-9_-]+\.html$/.test(shop.productPage.url));
const make=(tag,text)=>{const el=document.createElement(tag);if(text!==undefined)el.textContent=text;return el;};
let photos=[],busy=false;
// Interleave existing catalogue products for a varied, clearly labelled preview.
const collections=shops.map(shop=>({shop,products:[...(shop.products||[]),...(shop.productPage.products||[])].filter((p,i,all)=>p.image&&all.findIndex(other=>other.image===p.image)===i).slice(0,6)}));
const posts=[];
for(let i=0;i<6;i++)for(const {shop,products} of collections){const p=products[i];if(p)posts.push({shop,name:p.name,description:'',photos:[p.image],example:true});}
const previewKey='neighbourly.feed-preview.v1';
if($('#posts')){
 try{
  const raw=sessionStorage.getItem(previewKey);sessionStorage.removeItem(previewKey);
  const post=raw?JSON.parse(raw):null,shop=shops.find(s=>s.id===post?.shopId);
  if(shop&&typeof post.name==='string'&&post.name.trim()&&post.name.length<=120&&typeof post.description==='string'&&post.description.length<=1000&&Array.isArray(post.photos)&&post.photos.length>=1&&post.photos.length<=4&&post.photos.every(src=>typeof src==='string'&&src.length<=3000000&&/^data:image\/jpeg;base64,[A-Za-z0-9+/]+={0,2}$/.test(src)))posts.unshift({...post,shop,example:false});
 }catch{}
}
const form=$('#post-form'),status=$('#post-status'),publish=$('#publish');
if(form){
shops.forEach(shop=>{const option=make('option',shop.businessName);option.value=shop.id;$('#post-retailer').append(option);});
if(!shops.length){publish.disabled=true;status.textContent='No retailers have an enabled catalogue yet.';}
}
const pinMap=window.pincodeAreaData||{};
const quickButtons=[];
const allShops=window.marketplaceData.businesses||[];
const shopCity=shop=>shop.city||window.marketplaceData.config?.defaultCity||'Pune';
const selectedCity=()=>$('#feed-city').value||'Pune';
const cityShops=()=>allShops.filter(shop=>shopCity(shop)===selectedCity());
let pins=[];
function updatePins(){
 pins=[...new Set([...(selectedCity()==='Pune'?Object.keys(pinMap):[]),...cityShops().map(s=>s.pincode)])].filter(Boolean).sort();
 const select=$('#feed-pin');select.replaceChildren();const all=make('option','All pincodes');all.value='';select.append(all);select.value='';
 for(const pin of pins){const option=make('option',pin);option.value=pin;select.append(option);}
}
let areaOptions=[];
function updateAreas(){
 const pin=$('#feed-pin-input').value.trim(),select=$('#feed-area');
 const entries=selectedCity()==='Pune'?(pin?(pinMap[pin]||[]):Object.values(pinMap).flat()):[];
 const names=new Map(entries.map(item=>[item.label,{label:item.label,matches:[item.label,...(item.matches||[])]}]));
 for(const shop of cityShops().filter(s=>!pin||s.pincode===pin))if(!names.has(shop.area))names.set(shop.area,{label:shop.area,matches:[shop.area]});
 areaOptions=[...names.values()].sort((a,b)=>a.label.localeCompare(b.label));
 select.replaceChildren();const all=make('option','All areas');all.value='';select.append(all);select.value='';
 for(const area of areaOptions){const option=make('option',area.label);option.value=area.label;select.append(option);}
 $('#feed-location-help').textContent=pin&&!/^[1-9][0-9]{5}$/.test(pin)?'Enter a valid 6-digit pincode.':pin&&!areaOptions.length?'No areas listed for this pincode. Try another pincode.':'';
}
function resetFilters(){
 for(const id of ['#feed-category','#feed-pin-input','#feed-pin','#feed-area'])$(id).value='';$('#feed-city').value='Pune';updatePins();updateAreas();render();
}
function render(){
$('#posts').replaceChildren();
const category=$('#feed-category')?.value||'';
const pin=$('#feed-pin-input').value.trim(),area=areaOptions.find(a=>a.label===$('#feed-area').value);
quickButtons.forEach(button=>button.setAttribute('aria-pressed',String(button.dataset.category===category)));
const visible=posts.filter(post=>shopCity(post.shop)===selectedCity()&&(!pin||post.shop.pincode===pin)&&(!area||area.matches.some(name=>name.toLowerCase()===post.shop.area.toLowerCase()))&&(!category||post.shop.businessType===category));
$('#post-count').textContent=visible.length+' products';
if(!visible.length){const empty=make('div');empty.className='feed-empty';empty.append(make('h2','No products found'),make('p','No matching products here yet. Try another city, pincode, area or category.'));const reset=make('button','Clear filters');reset.type='button';reset.addEventListener('click',resetFilters);empty.append(reset);$('#posts').append(empty);}
visible.forEach(post=>{
const card=make('article');card.className='feed-card';const author=make('div');author.className='feed-author';
const avatar=make('img');avatar.src=post.shop.shopPhoto||'images/placeholder.svg';avatar.alt='';const info=make('div');const link=make('a',post.shop.businessName);link.href=post.shop.productPage.url;
info.append(link,make('small',(post.shop.area||'Local retailer')+' · '+(post.example?'Catalogue':'Preview')));author.append(avatar,info);
const gallery=make('div');gallery.className='feed-gallery';gallery.dataset.count=post.photos.length;
post.photos.forEach((src,i)=>{const button=make('button');button.type='button';button.setAttribute('aria-label','View '+post.name+' photo '+(i+1));const img=make('img');img.src=src;img.alt=post.name+' — photo '+(i+1);img.loading='lazy';button.append(img);button.addEventListener('click',()=>{const big=$('#feed-lightbox img');big.src=src;big.alt=img.alt;$('#feed-lightbox').showModal();});gallery.append(button);});
const body=make('div');body.className='feed-body';
body.append(make('h3',post.name));if(post.description)body.append(make('p',post.description));const categoryLabel=make('span',post.shop.businessType);categoryLabel.className='feed-card-category';body.append(categoryLabel);card.append(author,gallery,body);$('#posts').append(card);
});
}
function renderPhotos(){
$('#photo-previews').replaceChildren();$('#photo-count').textContent=photos.length+' / 4 photos';
photos.forEach((src,index)=>{const box=make('div'),img=make('img'),remove=make('button','×');img.src=src;img.alt='Selected photo '+(index+1);remove.type='button';remove.disabled=busy;remove.setAttribute('aria-label','Remove photo '+(index+1));remove.addEventListener('click',()=>{photos.splice(index,1);renderPhotos();});box.append(img,remove);$('#photo-previews').append(box);});
}
function readPhoto(file){return new Promise((resolve,reject)=>{
const url=URL.createObjectURL(file),img=new Image();
img.onerror=()=>{URL.revokeObjectURL(url);reject(Error('One photo could not be opened. Choose a valid JPG, PNG or WebP image.'));};
img.onload=()=>{try{const scale=Math.min(1,1400/Math.max(img.naturalWidth,img.naturalHeight)),canvas=document.createElement('canvas');canvas.width=Math.max(1,Math.round(img.naturalWidth*scale));canvas.height=Math.max(1,Math.round(img.naturalHeight*scale));const ctx=canvas.getContext('2d');ctx.fillStyle='#fff';ctx.fillRect(0,0,canvas.width,canvas.height);ctx.drawImage(img,0,0,canvas.width,canvas.height);resolve(canvas.toDataURL('image/jpeg',.85));}catch{reject(Error('Could not process this photo. Try a smaller image.'));}finally{URL.revokeObjectURL(url);}};img.src=url;
});}
if(form){
$('#post-photos').addEventListener('change',async event=>{
const input=event.target,files=Array.from(input.files);input.value='';if(!files.length)return;status.textContent='';
if(photos.length+files.length>4){status.textContent='A post can have a maximum of 4 photos. Remove a photo before adding more.';return;}
if(files.some(f=>!['image/jpeg','image/png','image/webp'].includes(f.type)||!f.size||f.size>5*1024*1024)){status.textContent='Choose JPG, PNG or WebP photos up to 5 MB each.';return;}
busy=true;publish.disabled=true;input.disabled=true;renderPhotos();
try{const added=await Promise.all(files.map(readPhoto));photos.push(...added);}catch(error){status.textContent=error.message;}finally{busy=false;publish.disabled=!shops.length;input.disabled=false;renderPhotos();}
});
form.addEventListener('submit',event=>{
event.preventDefault();if(busy||!form.reportValidity())return;
const shop=shops.find(s=>s.id===$('#post-retailer').value),name=$('#post-name').value.trim();
if(!shop||!name){status.textContent='Select a retailer and enter the product name.';return;}
if(photos.length<1||photos.length>4){status.textContent='Add between 1 and 4 product photos before publishing.';return;}
try{
 sessionStorage.setItem(previewKey,JSON.stringify({shopId:shop.id,name,description:$('#post-description').value.trim(),photos:[...photos]}));
 window.location.assign('product-feed.html');
}catch{status.textContent='Could not open this preview. Browser storage may be full or unavailable. Try fewer or smaller photos, or enable browser storage. Your form has been kept.';}

});
}
$('#feed-lightbox .dialog-close')?.addEventListener('click',()=>$('#feed-lightbox').close());
const menu=$('.menu-button');menu.addEventListener('click',()=>{const open=$('#navigation').classList.toggle('open');menu.setAttribute('aria-expanded',String(open));});
if($('#posts')){
 const category=$('#feed-category');
 for(const value of [...new Set([...(window.marketplaceData.categories||[]).map(c=>typeof c==='string'?c:c.name),...shops.map(shop=>shop.businessType)])].filter(Boolean)){const option=make('option',value);option.value=value;category.append(option);}
 const quick=['Grocery','Ladies Wear','Restaurants','Beauty & Cosmetics','Computer/Laptop shop'];
 for(const value of quick){const button=make('button',value);button.type='button';button.dataset.category=value;button.setAttribute('aria-pressed','false');button.addEventListener('click',()=>{category.value=category.value===value?'':value;render();});quickButtons.push(button);$('#feed-quick-categories').append(button);}
 const city=$('#feed-city');city.replaceChildren();
 const cities=[...new Set(['Pune','Mumbai','Delhi','Bengaluru','Hyderabad','Chennai','Kolkata','Ahmedabad','Jaipur','Surat','Lucknow','Chandigarh','Kochi','Indore',...allShops.map(shopCity)])];
 for(const name of cities){const option=make('option',name);option.value=name;city.append(option);}city.value='Pune';
 city.addEventListener('change',()=>{$('#feed-pin-input').value='';updatePins();updateAreas();render();});
 updatePins();
 $('#feed-pin-input').addEventListener('input',()=>{const pin=$('#feed-pin-input').value.trim();$('#feed-pin').value=pins.includes(pin)?pin:'';updateAreas();render();});
 $('#feed-pin').addEventListener('change',()=>{$('#feed-pin-input').value=$('#feed-pin').value;updateAreas();render();});
 $('#feed-area').addEventListener('change',render);$('#feed-reset').addEventListener('click',resetFilters);
 category.addEventListener('change',render);updateAreas();render();
}
})();
