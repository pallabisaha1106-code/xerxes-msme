/* Shared Neighbourly social links, independent of shop listings. */
(() => {
 const container=document.querySelector('.top-socials');
 if(!container)return;
 const links=window.neighbourlySocialLinks || {};
 for(const [key,name,icon] of contactPlatforms.filter(([key])=>['instagram','facebook','youtube','whatsapp'].includes(key))){
  const href=contactURL(links[key]);
  const item=document.createElement(href?'a':'button');
  item.className='top-social';
  if(href){item.href=href;item.target='_blank';item.rel='noopener noreferrer';}
  else{item.type='button';item.disabled=true;}
  const label='Neighbourly on '+name+(href?'':' — link coming soon');
  item.setAttribute('aria-label',label);item.title=label;
  item.innerHTML='<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">'+icon+'</svg>';
  container.append(item);
 }
})();
