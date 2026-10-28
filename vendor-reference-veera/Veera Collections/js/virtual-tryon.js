(function(){
  'use strict';
  const PRODUCTS=Object.freeze({
    'trial-dress-001':Object.freeze({name:'Teal Satin Midi Dress',productImage:'assets/catalog/teal-satin-midi-dress.jpeg',tryOnImage:'assets/virtual-tryon/teal-satin-midi-overlay.png'})
  });
  const modal=document.querySelector('[data-vto-modal]');
  if(!modal)return;
  const q=s=>modal.querySelector(s),qa=s=>[...modal.querySelectorAll(s)];
  const steps=qa('[data-step]'),status=q('[data-status]'),video=q('[data-camera-video]'),canvas=q('[data-preview-canvas]'),ctx=canvas.getContext('2d',{alpha:false}),fileInput=q('[data-photo-input]');
  let activeProduct=null,stream=null,photo=null,garment=null,objectUrl=null,lastFocus=null,drawState={x:0,y:0,scale:1},drag=null;
  const MAX_BYTES=10*1024*1024,MAX_PIXELS=20_000_000,ALLOWED=new Set(['image/jpeg','image/png','image/webp']);
  function announce(message,error=false){status.textContent=message;status.classList.toggle('is-error',error)}
  function showStep(name){steps.forEach(step=>step.hidden=step.dataset.step!==name);announce('')}
  function stopCamera(){if(stream){stream.getTracks().forEach(track=>track.stop());stream=null}video.pause();video.srcObject=null;video.removeAttribute('src');video.load()}
  function revokePhotoUrl(){if(objectUrl){URL.revokeObjectURL(objectUrl);objectUrl=null}}
  function clearCanvas(){if(ctx)ctx.clearRect(0,0,canvas.width,canvas.height);canvas.width=1;canvas.height=1}
  function clearMedia(){stopCamera();revokePhotoUrl();if(photo){photo.src='';photo=null}garment=null;clearCanvas();fileInput.value='';drag=null}
  function resetPosition(){if(!photo||!garment)return;drawState={x:0,y:0,scale:1};render()}
  function cleanup(){clearMedia();activeProduct=null;showStep('privacy')}
  function open(productId,trigger){const product=PRODUCTS[productId];if(!product)return;activeProduct=product;lastFocus=trigger;modal.hidden=false;document.body.classList.add('vto-open');showStep('privacy');q('[data-product-name]').textContent=product.name;requestAnimationFrame(()=>q('[data-continue]').focus())}
  function close(){cleanup();modal.hidden=true;document.body.classList.remove('vto-open');lastFocus?.focus();lastFocus=null}
  function fitCanvas(image){const max=1400,ratio=Math.min(1,max/image.naturalWidth,max/image.naturalHeight);canvas.width=Math.max(1,Math.round(image.naturalWidth*ratio));canvas.height=Math.max(1,Math.round(image.naturalHeight*ratio))}
  function garmentRect(){const baseWidth=canvas.width*.48*drawState.scale,ratio=garment.naturalHeight/garment.naturalWidth,height=baseWidth*ratio;return{x:(canvas.width-baseWidth)/2+drawState.x,y:canvas.height*.12+drawState.y,w:baseWidth,h:height}}
  function render(){if(!ctx||!photo||!garment)return;ctx.clearRect(0,0,canvas.width,canvas.height);ctx.drawImage(photo,0,0,canvas.width,canvas.height);const r=garmentRect();ctx.drawImage(garment,r.x,r.y,r.w,r.h)}
  function loadImage(src){return new Promise((resolve,reject)=>{const img=new Image();img.onload=()=>resolve(img);img.onerror=reject;img.src=src})}
  async function prepareEditor(photoImage){try{photo=photoImage;garment=await loadImage(activeProduct.tryOnImage);fitCanvas(photo);resetPosition();showStep('editor');announce('Preview ready. Adjust the dress position and size.')}catch(_){clearMedia();showStep('source');announce('The preview could not be created on this device. Please try another photo.',true)}}
  async function useCamera(){
    if(!window.isSecureContext){announce('Camera access requires the secure HTTPS version of this page. You can still choose a photo.',true);return}
    if(!navigator.mediaDevices?.getUserMedia){announce('Camera access is not supported by this browser. Please choose a photo instead.',true);return}
    stopCamera();showStep('camera');announce('Requesting camera access…');
    try{stream=await navigator.mediaDevices.getUserMedia({video:{facingMode:'user'},audio:false});video.srcObject=stream;await video.play();announce('Camera ready. Keep your full body visible, then capture.')}
    catch(error){stopCamera();showStep('source');const denied=error?.name==='NotAllowedError'||error?.name==='SecurityError';announce(denied?'Camera permission was not granted. You can choose a photo instead.':'The camera is unavailable or already in use. Please choose a photo or try again.',true)}
  }
  async function capture(){
    if(!stream||!video.videoWidth){announce('The camera is not ready yet. Please wait a moment.',true);return}
    try{const temp=document.createElement('canvas');temp.width=video.videoWidth;temp.height=video.videoHeight;const tempCtx=temp.getContext('2d');if(!tempCtx)throw new Error();tempCtx.translate(temp.width,0);tempCtx.scale(-1,1);tempCtx.drawImage(video,0,0);stopCamera();const blob=await new Promise(resolve=>temp.toBlob(resolve,'image/jpeg',.92));temp.width=temp.height=1;if(!blob)throw new Error();objectUrl=URL.createObjectURL(blob);const img=await loadImage(objectUrl);await prepareEditor(img)}catch(_){stopCamera();showStep('source');announce('The photo could not be captured. Please try again or choose a photo.',true)}
  }
  async function chooseFile(file){
    stopCamera();revokePhotoUrl();
    if(!file)return;
    const ext=(file.name.split('.').pop()||'').toLowerCase();
    if(!ALLOWED.has(file.type)||!['jpg','jpeg','png','webp'].includes(ext)){announce('Choose a valid JPEG, PNG or WebP image.',true);fileInput.value='';return}
    if(file.size===0||file.size>MAX_BYTES){announce(file.size>MAX_BYTES?'That photo is larger than 10 MB. Please choose a smaller image.':'That photo appears to be empty.',true);fileInput.value='';return}
    objectUrl=URL.createObjectURL(file);
    try{const img=await loadImage(objectUrl);if(img.naturalWidth<320||img.naturalHeight<320||img.naturalWidth*img.naturalHeight>MAX_PIXELS)throw new RangeError();await prepareEditor(img)}catch(error){revokePhotoUrl();fileInput.value='';announce(error instanceof RangeError?'Choose an image at least 320 × 320 pixels and no larger than 20 megapixels.':'That image could not be decoded. Please choose another photo.',true)}
  }
  function adjustPosition(direction){const amount=Math.max(6,canvas.width*.025);if(direction==='left')drawState.x-=amount;if(direction==='right')drawState.x+=amount;if(direction==='up')drawState.y-=amount;if(direction==='down')drawState.y+=amount;render()}
  function adjustSize(action){drawState.scale=Math.min(2,Math.max(.35,drawState.scale+(action==='increase'?.08:-.08)));render()}
  function pointerPoint(event){const rect=canvas.getBoundingClientRect();return{x:(event.clientX-rect.left)*canvas.width/rect.width,y:(event.clientY-rect.top)*canvas.height/rect.height}}
  function savePreview(){if(!photo)return;try{canvas.toBlob(blob=>{if(!blob){announce('This browser could not save the preview.',true);return}const url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download='veera-virtual-tryon-preview.png';a.click();setTimeout(()=>URL.revokeObjectURL(url),0);announce('Preview downloaded to your device. Veera Collections did not receive it.')},'image/png')}catch(_){announce('This browser could not save the preview.',true)}}
  function trapFocus(event){if(event.key==='Escape'){close();return}if(event.key!=='Tab')return;const items=qa('button:not([hidden]):not([disabled]),input:not([hidden]):not([disabled]),label[for]').filter(el=>el.offsetParent!==null);if(!items.length)return;const first=items[0],last=items[items.length-1];if(event.shiftKey&&document.activeElement===first){event.preventDefault();last.focus()}else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first.focus()}}
  document.querySelectorAll('[data-open-tryon]').forEach(button=>button.addEventListener('click',()=>open(button.dataset.productId,button)));
  qa('[data-close-tryon]').forEach(el=>el.addEventListener('click',close));
  q('[data-continue]').addEventListener('click',()=>showStep('source'));
  q('[data-back-privacy]').addEventListener('click',()=>showStep('privacy'));
  q('[data-use-camera]').addEventListener('click',useCamera);
  q('[data-capture]').addEventListener('click',capture);
  q('[data-camera-cancel]').addEventListener('click',()=>{stopCamera();showStep('source')});
  fileInput.addEventListener('change',event=>chooseFile(event.target.files?.[0]));
  qa('[data-move]').forEach(button=>button.addEventListener('click',()=>adjustPosition(button.dataset.move)));
  qa('[data-size]').forEach(button=>button.addEventListener('click',()=>adjustSize(button.dataset.size)));
  q('[data-reset-position]').addEventListener('click',resetPosition);
  q('[data-save]').addEventListener('click',savePreview);
  q('[data-retake]').addEventListener('click',()=>{clearMedia();showStep('source')});
  q('[data-reset-all]').addEventListener('click',()=>{cleanup();announce('Virtual Try-On reset. The photo and preview were removed.')});
  canvas.addEventListener('pointerdown',event=>{if(!photo)return;drag={start:pointerPoint(event),x:drawState.x,y:drawState.y};canvas.setPointerCapture(event.pointerId)});
  canvas.addEventListener('pointermove',event=>{if(!drag)return;const p=pointerPoint(event);drawState.x=drag.x+p.x-drag.start.x;drawState.y=drag.y+p.y-drag.start.y;render()});
  canvas.addEventListener('pointerup',()=>{drag=null});canvas.addEventListener('pointercancel',()=>{drag=null});
  modal.addEventListener('keydown',trapFocus);
  document.addEventListener('visibilitychange',()=>{if(document.hidden)stopCamera()});
  window.addEventListener('pagehide',cleanup);window.addEventListener('beforeunload',stopCamera);
})();
