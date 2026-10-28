
/* First-visit discovery preferences are stored only in this browser. */
window.initializeDiscoveryWelcome = function({businesses,categories,kind,onApply}) {
 const dialog=document.querySelector('#discovery-welcome');
 if(!dialog)return;
 const area=document.querySelector('#welcome-area'),category=document.querySelector('#welcome-category');
 const key='neighbourly.discovery.v1.'+kind;
 const areas=[...new Set(businesses.map(b=>b.area))].sort();
 let stored=null;
 try{stored=JSON.parse(localStorage.getItem(key));}catch{}
 const validArea=value=>areas.includes(value)?value:'';
 const validCategory=(value,location)=>value==='All'||businesses.some(b=>(!location||b.area===location)&&b.businessType===value)?value:'All';
 const save=selection=>{try{localStorage.setItem(key,JSON.stringify({seen:true,area:selection.area,category:selection.category}));}catch{}};
 function option(select,value,label){const node=document.createElement('option');node.value=value;node.textContent=label;select.append(node);}
 option(area,'','All areas');areas.forEach(value=>option(area,value,value));
 function refreshCategories(){
  const previous=category.value;
  category.innerHTML='';
  option(category,'All','All categories');
  categories.filter(c=>businesses.some(b=>(!area.value||b.area===area.value)&&b.businessType===c.name)).forEach(c=>option(category,c.name,c.name));
  category.value=validCategory(previous,area.value);
 }
 area.addEventListener('change',refreshCategories);
 area.value='';refreshCategories();
 if(stored?.seen===true){
  const selectedArea=validArea(stored.area);
  onApply({area:selectedArea,category:validCategory(stored.category,selectedArea)});
  window.saveDiscoverySelection=save;
  return;
 }
 let applied=false;
 document.querySelector('#welcome-form').addEventListener('submit',event=>{
  event.preventDefault();
  const selection={area:validArea(area.value),category:validCategory(category.value,area.value)};
  applied=true;save(selection);window.saveDiscoverySelection=save;
  onApply(selection);dialog.close();
  document.querySelector('#search').focus();
 });
 document.querySelector('#welcome-skip').addEventListener('click',()=>dialog.close());
 dialog.addEventListener('close',()=>{
  if(!applied){save({area:'',category:'All'});window.saveDiscoverySelection=save;}
 });
 dialog.showModal();
};

