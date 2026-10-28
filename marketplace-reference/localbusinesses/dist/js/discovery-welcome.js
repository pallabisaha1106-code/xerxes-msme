/* First-visit discovery preferences are stored only in this browser. */
window.initializeDiscoveryWelcome = function({businesses,categories,kind,welcomeCategories,alwaysShow,onApply}) {
 const dialog=document.querySelector('#discovery-welcome');
 if(!dialog)return;
 const pincode=document.querySelector('#welcome-pincode'),category=document.querySelector('#welcome-category'),cities=document.querySelector('#welcome-cities');
 const key='neighbourly.discovery.v2.'+kind;
 const visitKey='neighbourly.discovery.session.'+kind;
 const majorCities=['Pune','Mumbai','Nagpur','Nashik'];
 const allowedNames=Array.isArray(welcomeCategories)&&welcomeCategories.length?welcomeCategories:categories.map(item=>item.name);
 const availableCategories=allowedNames.map(name=>categories.find(item=>item.name===name)).filter(Boolean);
 let selectedCity='',stored=null;
 try{stored=JSON.parse(localStorage.getItem(key));}catch{}
 let showForEntryOrReload=false;
 if(alwaysShow){
  let visited=false;
  try{visited=sessionStorage.getItem(visitKey)==='true';sessionStorage.setItem(visitKey,'true');}catch{}
  const navigationType=performance.getEntriesByType?.('navigation')?.[0]?.type;
  showForEntryOrReload=!visited||navigationType==='reload';
 }
 const validPincode=value=>/^[1-9][0-9]{5}$/.test(String(value||''))?String(value):'';
 const validCity=value=>majorCities.includes(value)?value:'';
 const validCategory=value=>value==='All'||availableCategories.some(item=>item.name===value)?value:'All';
 const save=selection=>{try{localStorage.setItem(key,JSON.stringify({seen:true,pincode:selection.pincode,city:selection.city,category:selection.category}));}catch{}};
 function option(select,value,label){const node=document.createElement('option');node.value=value;node.textContent=label;select.append(node);}
 option(category,'All',Array.isArray(welcomeCategories)&&welcomeCategories.length?'All four categories':'All categories');
 availableCategories.forEach(item=>option(category,item.name,item.name));
 cities.innerHTML=majorCities.map(city=>'<button type="button" class="welcome-city" data-city="'+city+'" aria-pressed="false">'+city+'</button>').join('');
 function chooseCity(city){
  selectedCity=validCity(city);
  if(selectedCity)pincode.value='';
  cities.querySelectorAll('.welcome-city').forEach(button=>button.setAttribute('aria-pressed',String(button.dataset.city===selectedCity)));
 }
 cities.addEventListener('click',event=>{const button=event.target.closest('.welcome-city');if(button)chooseCity(button.dataset.city);});
 pincode.addEventListener('input',()=>{pincode.value=pincode.value.replace(/\D/g,'').slice(0,6);if(pincode.value)chooseCity('');});
 if(stored?.seen===true&&!showForEntryOrReload){
  onApply({pincode:validPincode(stored.pincode),city:validCity(stored.city),category:validCategory(stored.category)});
  window.saveDiscoverySelection=save;
  return;
 }
 let applied=false;
 document.querySelector('#welcome-form').addEventListener('submit',event=>{
  event.preventDefault();
  const entered=pincode.value.trim();
  if(entered&&!validPincode(entered)){pincode.setCustomValidity('Enter a valid 6-digit Indian pincode.');pincode.reportValidity?.();return;}
  pincode.setCustomValidity('');
  const selection={pincode:validPincode(entered),city:selectedCity,category:validCategory(category.value)};
  applied=true;save(selection);window.saveDiscoverySelection=save;
  onApply(selection);dialog.close();
  document.querySelector('#search').focus();
 });
 document.querySelector('#welcome-skip').addEventListener('click',()=>dialog.close());
 dialog.addEventListener('close',()=>{
  if(!applied){save({pincode:'',city:'',category:'All'});window.saveDiscoverySelection=save;}
 });
 dialog.showModal();
};
