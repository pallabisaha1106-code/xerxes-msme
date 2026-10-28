function productPricePaise(product) {
 return typeof product.mrp==='number' && Number.isFinite(product.mrp) && product.mrp>=0 && Number.isSafeInteger(Math.round(product.mrp*100)) ? Math.round(product.mrp*100) : null;
}
function formatPrice(paise) {return new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR'}).format(paise/100);}
function productPack(product) {return product.name+(product.packSize?' ('+product.packSize+')':'');}

function isGroceryShop(shop) {
 return ['grocery','grocery shop'].includes(String(shop.businessType||'').trim().toLowerCase());
}
function createGroceryCart(shop,products) {
 const quantities=new Map();
 return {
  set(index,quantity){if(!isGroceryShop(shop)||!Number.isInteger(index)||!products[index])return;const n=Number(quantity);if(!Number.isInteger(n)||n<0||n>999)return;if(n===0)quantities.delete(index);else quantities.set(index,n);},
  quantity(index){return quantities.get(index)||0;},
  entries(){return [...quantities].map(([index,quantity])=>({index,quantity,product:products[index]}));},
  clear(){quantities.clear();},
  total(){const entries=this.entries();return entries.some(e=>productPricePaise(e.product)===null)?null:entries.reduce((sum,e)=>sum+productPricePaise(e.product)*e.quantity,0);},
  url(){
   const number=String(shop.enquiryWhatsapp||'').replace(/[+\s()-]/g,'');
   if(!isGroceryShop(shop)||!quantities.size||!/^[1-9]\d{7,14}$/.test(number))return '';
   const lines=this.entries().map(({index,quantity,product},i)=>(i+1)+'. '+productPack(product)+' | Ref: '+(product.id||String(index+1))+' | Quantity: '+quantity+(productPricePaise(product)===null?' | Price on enquiry':' | Unit MRP: '+formatPrice(productPricePaise(product))+' | Total: '+formatPrice(productPricePaise(product)*quantity)));
   return 'https://wa.me/'+number+'?text='+encodeURIComponent('Hello '+shop.businessName+', I would like to enquire about these products from Neighbourly:\n\n'+lines.join('\n')+'\n\nGrand total: '+(this.total()===null?'Awaiting prices':formatPrice(this.total()))+'\n\nPlease confirm prices and availability. Thank you.');
  }
 };
}
function mountGroceryCart(shop,products,grid) {
 const cart=createGroceryCart(shop,products);
 const panel=document.createElement('section');panel.className='grocery-cart';panel.setAttribute('aria-label','Your grocery cart');
 const title=document.createElement('h2');title.textContent='Your cart';
 const status=document.createElement('p');status.setAttribute('role','status');
 const items=document.createElement('div');items.className='grocery-cart-items';
 const total=document.createElement('p');total.className='grocery-cart-total';total.setAttribute('aria-live','polite');
 const actions=document.createElement('div');actions.className='grocery-cart-actions';
 const send=document.createElement('a');send.className='product-enquiry';send.textContent='Send cart on WhatsApp';send.target='_blank';send.rel='noopener noreferrer';
 const clear=document.createElement('button');clear.type='button';clear.textContent='Clear cart';
 const note=document.createElement('p');note.className='grocery-cart-note';note.textContent='Quantities are for each listed product or pack. The shop will confirm prices and availability. You send the message in WhatsApp.';
 actions.append(send,clear);panel.append(title,status,items,actions,note,total);grid.before(panel);
 function render(){
  const entries=cart.entries();status.textContent=entries.length?entries.reduce((sum,e)=>sum+e.quantity,0)+' items across '+entries.length+' products':'Your cart is empty. Add products below.';
  total.hidden=!entries.length;total.textContent='Grand total: '+(cart.total()===null?'Awaiting prices':formatPrice(cart.total()));
  items.replaceChildren();
  entries.forEach(({index,quantity,product})=>{
   const row=document.createElement('div');row.className='grocery-cart-row';
   const label=document.createElement('span');label.textContent=productPack(product)+' · Ref '+(product.id||String(index+1))+(productPricePaise(product)===null?' · Price on enquiry':' · Unit MRP: '+formatPrice(productPricePaise(product))+' · Total: '+formatPrice(productPricePaise(product)*quantity));
   const controls=document.createElement('div');controls.className='grocery-quantity';
   const minus=document.createElement('button');minus.type='button';minus.textContent='−';minus.setAttribute('aria-label','Decrease quantity of '+product.name);
   const input=document.createElement('input');input.type='number';input.min='1';input.max='999';input.step='1';input.value=quantity;input.setAttribute('aria-label','Quantity for '+product.name);
   const plus=document.createElement('button');plus.type='button';plus.textContent='+';plus.disabled=quantity>=999;plus.setAttribute('aria-label','Increase quantity of '+product.name);
   const remove=document.createElement('button');remove.type='button';remove.textContent='Remove';remove.setAttribute('aria-label','Remove '+product.name);
   minus.addEventListener('click',()=>{cart.set(index,quantity-1);render();});
   plus.addEventListener('click',()=>{cart.set(index,quantity+1);render();});
   input.addEventListener('change',()=>{cart.set(index,Number(input.value));render();});
   remove.addEventListener('click',()=>{cart.set(index,0);render();});
   controls.append(minus,input,plus,remove);row.append(label,controls);items.append(row);
  });
  const url=cart.url();send.hidden=!url;clear.hidden=!entries.length;
  if(url)send.href=url;else send.removeAttribute('href');
  if(entries.length&&!url)status.textContent+=' · WhatsApp enquiries are not available for this shop yet.';
 }
 clear.addEventListener('click',()=>{cart.clear();render();});
 render();
 return {add(index){cart.set(index,Math.min(999,cart.quantity(index)+1));render();}};
}

function productEnquiryURL(shop, product, reference) {
 const number=String(shop.enquiryWhatsapp || '').replace(/[+\s()-]/g,'');
 if(!/^[1-9]\d{7,14}$/.test(number))return '';
 const message='Hello '+shop.businessName+', I found this product on Neighbourly.\nProduct: '+product.name+'\nRef: '+reference+'\nPlease share the price, available sizes and availability.';
 return 'https://wa.me/'+number+'?text='+encodeURIComponent(message);
}

/* Expanded catalogues are opt-in per shop, with no query-string fallback. */
(() => {
 const shop=businesses.find(b=>b.id===document.body.dataset.productShop);
 const enabled=shop && productPageURL(shop) && location.pathname.split('/').pop()===shop.productPage.url;
 const content=document.querySelector('#catalogue-content');
 if(!enabled){document.querySelector('#catalogue-unavailable').hidden=false;}
 else{
  content.hidden=false;
  const query=s=>document.querySelector(s);
  query('#catalogue-shop-name').textContent=shop.businessName;
  query('#catalogue-address').textContent=shop.address;
  query('#catalogue-shop-image').src=safeURL(shop.shopPhoto || 'images/placeholder.svg');
  query('#catalogue-shop-image').alt=shop.shopPhotoAlt || shop.businessName;
  query('#catalogue-contact').innerHTML=renderContactLinks(shop);
  const products=[...shop.products.map((p,i)=>({...p,id:p.id || (shop.id==='veera'?'VC':shop.id.toUpperCase())+'-'+String(i+1).padStart(2,'0'),category:p.category || p.name})),...(shop.productPage.products||[])];
  query('#catalogue-count').textContent=products.length+' products';
  const grid=query('#catalogue-grid');
  const groceryCart=isGroceryShop(shop)?mountGroceryCart(shop,products,grid):null;
  const dialog=query('#catalogue-preview');
  products.forEach((product,i)=>{
   const card=document.createElement('article');card.className='catalogue-product';
   const button=document.createElement('button');button.className='catalogue-photo';button.type='button';
   button.setAttribute('aria-label','Preview '+product.name);
   const image=document.createElement('img');image.src=safeURL(product.image || 'images/placeholder.svg');image.alt=product.name;image.loading='lazy';image.width=600;image.height=800;
   image.addEventListener('error',()=>{image.src='images/placeholder.svg';},{once:true});
   button.append(image);
   const heading=document.createElement('h3');heading.textContent=product.name;
   const label=document.createElement('p');label.textContent=(product.category || 'Collection')+' · Ref '+(product.id || String(i+1));
   card.append(button,heading,label);
   if(isGroceryShop(shop)){const price=document.createElement('p');price.className='product-price';price.textContent=(product.packSize?product.packSize+' · ':'')+(productPricePaise(product)===null?'Price on enquiry':'MRP '+formatPrice(productPricePaise(product)));card.append(price);}
   const enquiryURL=productEnquiryURL(shop,product,product.id || String(i+1));
   if(enquiryURL){
    const enquiry=document.createElement('a');enquiry.className='product-enquiry';enquiry.href=enquiryURL;enquiry.target='_blank';enquiry.rel='noopener noreferrer';enquiry.textContent='WhatsApp enquiry';
    enquiry.setAttribute('aria-label','Enquire with '+shop.businessName+' on WhatsApp about '+product.name+', Ref '+(product.id || String(i+1)));
    card.append(enquiry);
   }
   if(groceryCart){
    const add=document.createElement('button');add.type='button';add.className='product-add-cart';add.textContent='Add to cart';add.setAttribute('aria-label','Add '+product.name+' to cart');add.addEventListener('click',()=>groceryCart.add(i));card.append(add);
   }
   grid.append(card);
   button.addEventListener('click',()=>{
    query('#catalogue-preview-image').src=safeURL(product.image || 'images/placeholder.svg');
    query('#catalogue-preview-image').alt=product.name;
    query('#catalogue-preview-title').textContent=product.name;
    query('#catalogue-preview-code').textContent=shop.businessName+' · Ref '+(product.id || String(i+1));
    dialog.showModal();
   });
  });
  query('#catalogue-preview-image').addEventListener('error',()=>{if(!query('#catalogue-preview-image').src.endsWith('/placeholder.svg'))query('#catalogue-preview-image').src='images/placeholder.svg';});
  query('#catalogue-shop-image').addEventListener('error',()=>{query('#catalogue-shop-image').src='images/placeholder.svg';},{once:true});
  dialog.querySelector('.dialog-close').addEventListener('click',()=>dialog.close());
  dialog.addEventListener('click',event=>{if(event.target!==dialog)return;const r=dialog.getBoundingClientRect();if(event.clientX<r.left||event.clientX>r.right||event.clientY<r.top||event.clientY>r.bottom)dialog.close();});
 }
 const menu=document.querySelector('.menu-button'),nav=document.querySelector('#navigation');
 menu.addEventListener('click',()=>{menu.setAttribute('aria-expanded',nav.classList.toggle('open'));});
 document.querySelector('#year').textContent=new Date().getFullYear();
})();

