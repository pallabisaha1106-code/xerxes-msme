/* Owner drafts are local; publication stays with the administrator. */
function groceryPrice(value) {
 const text=String(value).trim();
 return /^\d{1,7}(?:\.\d{1,2})?$/.test(text)&&Number(text)>0?Number(text):null;
}
function groceryExport(shopName, products) {
 if(!shopName.trim())throw new Error('Enter your grocery shop name.');
 if(!products.length)throw new Error('Select at least one product.');
 return {schemaVersion:1,businessType:'Grocery',businessName:shopName.trim(),products:products.map(p=>{
  const mrp=groceryPrice(p.price);
  if(!p.name.trim()||!p.category||mrp===null||!p.quantity.trim())throw new Error('Enter a valid price and pack quantity for '+p.name+'.');
  return {id:p.id,name:p.name,category:p.category,mrp,packSize:p.quantity.trim(),image:p.photo||'',custom:!!p.custom};
 })};
}
(() => {
 const $=s=>document.querySelector(s),key='neighbourly-grocery-owner-draft-v1',selected=new Map();
 const defaults=Object.entries(window.groceryDefaults).flatMap(([category,names],c)=>names.map((name,i)=>({id:'GROC-'+(c+1)+'-'+(i+1),name,category,photo:window.groceryProductImages?.['GROC-'+(c+1)+'-'+(i+1)]||''})));
 const status=message=>$('#builder-status').textContent=message;
 const make=(tag,text)=>{const el=document.createElement(tag);if(text!==undefined)el.textContent=text;return el;};
 for(const category of Object.keys(window.groceryDefaults)){
  for(const select of [$('#category-filter'),$('#custom-product').elements.category]){const option=make('option',category);option.value=category;select.append(option);}
 }
 function renderDefaults(){
  const term=$('#product-search').value.trim().toLowerCase(),category=$('#category-filter').value;
  const visible=defaults.filter(p=>(!category||p.category===category)&&(!term||(p.name+' '+p.category).toLowerCase().includes(term)));
  $('#default-count').textContent=visible.length+' products available';
  const root=$('#default-products');root.replaceChildren();
  for(const group of Object.keys(window.groceryDefaults)){
   const products=visible.filter(p=>p.category===group);if(!products.length)continue;
   const section=make('section');section.className='default-group';section.append(make('h3',group));
   products.forEach(p=>{
    const label=make('label');label.className='default-choice';const checkbox=make('input');checkbox.type='checkbox';checkbox.checked=selected.has(p.id);
    checkbox.addEventListener('change',()=>{if(checkbox.checked)selected.set(p.id,{...p,price:'',quantity:'',custom:false});else selected.delete(p.id);renderSelected();});
    const photo=make('img');photo.src=p.photo||'images/placeholder.svg';photo.alt='';photo.width=42;photo.height=48;photo.loading='lazy';
    label.append(checkbox,photo,make('span',p.name));section.append(label);
   });root.append(section);
  }
  if(!visible.length)root.append(make('p','No matching products. Add a custom product below.'));
 }
 function renderSelected(){
  const root=$('#selected-products');root.replaceChildren();$('#selected-count').textContent=selected.size;
  if(!selected.size){const row=make('tr'),cell=make('td','Select products from the list to start your catalogue.');cell.colSpan=4;row.append(cell);root.append(row);};
  for(const p of selected.values()){
   const card=make('tr');const name=make('td',p.name);name.title=p.name+' · '+p.category;card.append(name);
   if(p.photo){const image=make('img');image.src=p.photo;image.alt=p.name;name.append(image);}
   const fields=[];
   for(const [field,title,placeholder] of [['price','Price / MRP per pack (₹)','e.g. 125.00'],['quantity','Pack quantity','e.g. 1 kg']]){
    const label=make('td'),input=make('input');input.setAttribute('aria-label',title+' for '+p.name);input.type='text';input.value=p[field];input.placeholder=placeholder;input.maxLength=field==='price'?10:60;if(field==='price')input.inputMode='decimal';
    input.addEventListener('input',()=>{p[field]=input.value;status('Unsaved changes. Save your draft or submit when ready.');});label.append(input);fields.push(label);
   }
   const remove=make('button','×');remove.title='Remove '+p.name;remove.type='button';remove.setAttribute('aria-label','Remove '+p.name);remove.addEventListener('click',()=>{selected.delete(p.id);renderSelected();renderDefaults();});
   const actions=make('td');actions.append(remove);card.append(...fields,actions);root.append(card);
  }
 }
 async function photoData(file){
  if(!file||!['image/jpeg','image/png','image/webp'].includes(file.type)||file.size>5*1024*1024)throw new Error('Choose a JPG, PNG or WebP photo under 5 MB.');
  // Decode the upload before storing it, and keep drafts small enough for local storage.
  const bitmap=await createImageBitmap(file);
  const scale=Math.min(1,800/Math.max(bitmap.width,bitmap.height)),canvas=document.createElement('canvas');canvas.width=Math.round(bitmap.width*scale);canvas.height=Math.round(bitmap.height*scale);
  const context=canvas.getContext('2d');context.fillStyle='#fff';context.fillRect(0,0,canvas.width,canvas.height);context.drawImage(bitmap,0,0,canvas.width,canvas.height);bitmap.close();
  return canvas.toDataURL('image/jpeg',0.82);
 }
 $('#custom-product').addEventListener('submit',async event=>{
  event.preventDefault();const form=event.currentTarget,fields=form.elements,button=form.querySelector('button');
  try{
   if(!fields.name.value.trim()||!fields.quantity.value.trim()||groceryPrice(fields.price.value)===null)throw new Error('Enter a product name, positive price and pack quantity.');
   button.disabled=true;$('#custom-status').textContent='Preparing photo…';
   const photo=await photoData(fields.photo.files[0]),id='CUSTOM-'+crypto.randomUUID();
   selected.set(id,{id,name:fields.name.value.trim(),category:fields.category.value,price:fields.price.value.trim(),quantity:fields.quantity.value.trim(),photo,custom:true});
   form.reset();renderSelected();$('#custom-status').textContent='Custom product added to your catalogue.';
  }catch(error){$('#custom-status').textContent=error.message;}finally{button.disabled=false;}
 });
 $('#save-catalogue').addEventListener('click',()=>{
  try{localStorage.setItem(key,JSON.stringify({shopName:$('#owner-shop').value,products:[...selected.values()]}));status('Draft saved in this browser.');}
  catch{status('This browser could not save the draft. Download your catalogue to keep a copy.');}
 });
 $('#submit-catalogue').addEventListener('click',async ()=>{
 const button=$('#submit-catalogue');try{
 const data=groceryExport($('#owner-shop').value,[...selected.values()]);button.disabled=true;status('Submitting catalogue…');
 const response=await fetch('/api/grocery-catalogues',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(data)});
 const result=await response.json();if(!response.ok||!result.ok)throw new Error(result.error||'The catalogue was not saved.');
 status('Catalogue saved to the server. Reference: '+result.reference);
 }catch(error){status('Submission failed: '+error.message+' Your catalogue is still here. Please retry.');}finally{button.disabled=false;}
 });
 $('#download-catalogue').addEventListener('click',()=>{
  try{
   const data=groceryExport($('#owner-shop').value,[...selected.values()]);
   const url=URL.createObjectURL(new Blob([JSON.stringify(data,null,2)],{type:'application/json'})),a=make('a');a.href=url;a.download='grocery-catalogue.json';document.body.append(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),1000);
   status('Catalogue downloaded. Share the JSON file with the Neighbourly team; custom photos are included.');
  }catch(error){status(error.message);}
 });
 try{
  const saved=JSON.parse(localStorage.getItem(key)||'null');
  if(saved&&typeof saved.shopName==='string'&&Array.isArray(saved.products)){
   $('#owner-shop').value=saved.shopName.slice(0,120);
   for(const p of saved.products){
    if(!p)continue;
    const known=defaults.find(d=>d.id===p.id);
    if(!p||typeof p.id!=='string'||!known&&!p.custom||!['name','category','price','quantity','photo'].every(k=>typeof p[k]==='string'))continue;
    if(!Object.hasOwn(window.groceryDefaults,p.category))continue;
    if(!known&&p.photo&&!/^data:image\/jpeg;base64,[A-Za-z0-9+/=]+$/.test(p.photo))continue;
    selected.set(p.id,{...p,...(known||{})});
   }status('Your saved draft has been restored.');
  }
 }catch{status('No saved draft could be restored. Start a new catalogue below.');}
 $('#product-search').addEventListener('input',renderDefaults);$('#category-filter').addEventListener('change',renderDefaults);
 renderDefaults();renderSelected();
})();

