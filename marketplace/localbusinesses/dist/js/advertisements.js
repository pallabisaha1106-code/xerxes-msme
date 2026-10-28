/* Editable advertisement rotation; no dependencies. */
(() => {
 const root=document.querySelector('#top-advertisement');
 if(!root)return;
 const settings=window.marketplaceAds || {};
 const slides=(settings.slides || []).filter(slide=>slide && typeof slide.image==='string' && slide.image.trim());
 if(!slides.length){root.hidden=true;return;}
 const safeLink=value=>{try{const u=new URL(value);return /^https?:$/.test(u.protocol)&&!u.username&&!u.password?u.href:'';}catch{return '';}};
 const safeImage=value=>{try{const u=new URL(value,location.href);return ['http:','https:','file:'].includes(u.protocol)?u.href:'';}catch{return '';}};
 const track=root.querySelector('.ad-slides');
 let index=0,focused=false,timer;
 const panels=slides.map((slide,i)=>{
  const panel=document.createElement('a'),url=safeLink(slide.url);
  panel.className=slide.imageOnly?'banner-slide banner-image-only':'banner-slide';panel.hidden=i!==0;
  if(url){panel.href=url;panel.target='_blank';panel.rel='noopener noreferrer';}
  panel.setAttribute('aria-label',slide.shopName+' — '+slide.headline+(url?' (opens in a new tab)':''));
  const copy=document.createElement('div');copy.className='banner-copy';
  for(const [tag,className,text] of [['span','eyebrow',slide.shopName],['h2','',slide.headline],['p','',slide.description],['span','banner-cta',url?'Explore collection ↗':'']]){
   const item=document.createElement(tag);item.className=className;item.textContent=text||'';copy.append(item);
  }
  const img=document.createElement('img');img.src=safeImage(slide.image)||'images/placeholder.svg';img.alt=slide.alt||slide.shopName||'Advertisement';
  img.width=600;img.height=700;img.loading=i===0?'eager':'lazy';
  img.addEventListener('error',()=>{img.src='images/placeholder.svg';},{once:true});
  if(!slide.imageOnly)panel.append(copy);
  panel.append(img);track.append(panel);
  return panel;
 });
 function show(next){
  index=(next+panels.length)%panels.length;
  panels.forEach((panel,i)=>{panel.hidden=i!==index;});
 }
 function schedule(){
  clearInterval(timer);
  if(!focused&&!document.hidden&&panels.length>1)timer=setInterval(()=>show(index+1),Math.max(3000,Number(settings.intervalMs)||3000));
 }
 root.addEventListener('focusin',()=>{focused=true;schedule();});
 root.addEventListener('focusout',event=>{if(!root.contains(event.relatedTarget)){focused=false;schedule();}});
 document.addEventListener('visibilitychange',schedule);
 show(0);schedule();
})();
