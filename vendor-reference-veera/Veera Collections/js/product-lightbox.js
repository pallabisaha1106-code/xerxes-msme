(function () {
  'use strict';
  if (window.VeeraProductPhotoPopup) return;
  window.VeeraProductPhotoPopup = true;
  const root = new URL('../', document.currentScript.src);
  const legacyNames = {
    'abstract-print-kaftan-dress.jpeg': 'Abstract print kaftan',
    'black-belted-coord-set.jpeg': 'Midnight Belted Co-ord Set',
    'black-floral-embroidered-coord-set.jpeg': 'Black Floral Embroidered Co-ord Set',
    'black-satin-maxi-dress.jpeg': 'Black Satin Maxi Dress',
    'blue-layered-dress-set.jpeg': 'Blue Layered Dress Set',
    'burgundy-draped-maxi-dress.jpeg': 'Burgundy Draped Maxi Dress',
    'floral-embellished-palazzo-set.jpeg': 'Floral Embellished Palazzo Set',
    'ivory-gold-print-kurta-set.jpeg': 'Ivory Gold Print Kurta Set',
    'marble-print-midi-dress.jpeg': 'Marble Print Midi Dress',
    'maroon-strappy-maxi-dress.jpeg': 'Maroon Strappy Maxi Dress',
    'midnight-floral-kaftan-dress.jpeg': 'Midnight Print Kaftan Dress',
    'mint-embroidered-anarkali-set.jpeg': 'Mint Embroidered Anarkali Set',
    'peacock-floral-kurta-set.jpeg': 'Peacock Floral Kurta Set',
    'powder-blue-kurta.jpeg': 'Powder Blue Kurta',
    'rose-gold-kurta-set.jpeg': 'Rose Gold Kurta Set',
    'rose-pink-floral-kurta-set.jpeg': 'Rose Pink Floral Kurta Set',
    'teal-satin-midi-dress.jpeg': 'Teal Satin Midi Dress'
  };
  const absolute = path => new URL(path, root).href;
  const relative = url => {
    try { const u = new URL(url, root); return u.origin === root.origin && u.pathname.startsWith(root.pathname) ? u.pathname.slice(root.pathname.length) : ''; }
    catch (_) { return ''; }
  };
  const productPath = path => /^assets\/(?:catalog\/[^/]+\.(?:jpeg|jpg|png|webp)|category-products\/vc-[a-f0-9]+(?:-480)?\.webp)$/i.test(path);
  const fullPath = path => path.replace(/-480(?=\.webp$)/, '');
  let recordsPromise;
  function records() {
    if (!recordsPromise) recordsPromise = fetch(absolute('data/category-products.json')).then(response => {
      if (!response.ok) throw new Error('Product data unavailable');
      return response.json();
    }).then(data => {
      const byImage = new Map();
      data.products.forEach(p => {
        byImage.set(p.image, p);
        (p.alternateImages || []).forEach(photo => byImage.set(photo.image, p));
      });
      return byImage;
    }).catch(() => { recordsPromise = undefined; return new Map(); });
    return recordsPromise;
  }
  function eligible(img) {
    if (!(img instanceof HTMLImageElement) || img.closest('.vc-photo-dialog,[data-vto-modal]')) return false;
    if (!productPath(relative(img.getAttribute('src') || ''))) return false;
    const anchor = img.closest('a');
    // Category tiles, social links and other navigation keep their original action.
    return !anchor || productPath(relative(anchor.href));
  }
  function prepare(img) {
    if (!eligible(img) || typeof HTMLDialogElement === 'undefined' || !HTMLDialogElement.prototype.showModal) return;
    const trigger = img.closest('a') || img;
    if (trigger.hasAttribute('data-vc-photo-trigger')) return;
    trigger.setAttribute('data-vc-photo-trigger', '');
    trigger.setAttribute('aria-haspopup', 'dialog');
    if (trigger === img) {
      img.setAttribute('tabindex', '0');
      img.setAttribute('role', 'button');
      img.setAttribute('aria-label', 'View product photo' + (img.alt ? ': ' + img.alt : ''));
    }
  }
  function discover(node) {
    if (!(node instanceof Element)) return;
    if (node.matches('img')) prepare(node);
    node.querySelectorAll('img').forEach(prepare);
  }
  let dialog, stage, photo, status, title, description, category, colour, zoomIn, zoomOut, reset, zoomLabel, closeButton;
  let trigger, scale = 1, panX = 0, panY = 0, fitWidth = 0, fitHeight = 0, loaded = false, drag = null, version = 0, backdropDown = false, scrollState;
  function lockScroll() {
    const html = document.documentElement;
    scrollState = { x: window.scrollX, y: window.scrollY, overflow: html.style.overflow, gutter: html.style.scrollbarGutter };
    const gap = window.innerWidth - html.clientWidth;
    if (!gap || CSS.supports('scrollbar-gutter', 'stable')) {
      if (gap) html.style.scrollbarGutter = 'stable';
      html.style.overflow = 'hidden';
    }
  }
  function unlockScroll() {
    if (!scrollState) return;
    const html = document.documentElement, saved = scrollState;
    html.style.overflow = saved.overflow; html.style.scrollbarGutter = saved.gutter;
    if (window.scrollX !== saved.x || window.scrollY !== saved.y) window.scrollTo({ left: saved.x, top: saved.y, behavior: 'instant' });
    scrollState = null;
  }
  function paint() {
    const maxX = Math.max(0, (fitWidth * scale - stage.clientWidth) / 2);
    const maxY = Math.max(0, (fitHeight * scale - stage.clientHeight) / 2);
    panX = Math.max(-maxX, Math.min(maxX, panX)); panY = Math.max(-maxY, Math.min(maxY, panY));
    photo.style.transform = `translate(-50%, -50%) translate(${panX}px, ${panY}px) scale(${scale})`;
    stage.dataset.zoomed = String(loaded && scale > 1);
    zoomIn.disabled = !loaded || scale >= 4;
    zoomOut.disabled = !loaded || scale <= 1;
    reset.disabled = !loaded || (scale === 1 && panX === 0 && panY === 0);
    zoomLabel.textContent = scale.toFixed(1).replace('.0', '') + '×';
  }
  function fit() {
    if (!loaded || !photo.naturalWidth || !stage.clientWidth) return;
    const ratio = Math.min(stage.clientWidth / photo.naturalWidth, stage.clientHeight / photo.naturalHeight);
    fitWidth = photo.naturalWidth * ratio; fitHeight = photo.naturalHeight * ratio;
    photo.style.width = fitWidth + 'px'; photo.style.height = fitHeight + 'px'; paint();
  }
  function close() {
    if (!dialog || !dialog.open) return;
    version++; drag = null; stage.dataset.dragging = 'false';
    dialog.close(); photo.removeAttribute('src'); unlockScroll();
    if (trigger && trigger.isConnected) trigger.focus({ preventScroll: true });
  }
  function create() {
    if (dialog) return;
    dialog = document.createElement('dialog');
    dialog.className = 'vc-photo-dialog'; dialog.setAttribute('aria-labelledby', 'vc-photo-dialog-title');
    dialog.setAttribute('aria-modal', 'true');
    dialog.innerHTML = '<section class="vc-photo-panel"><div class="vc-photo-heading"><h2 id="vc-photo-dialog-title">Product photo</h2><button class="vc-photo-button" type="button" data-vc-close aria-label="Close product photo popup">Close ×</button></div><div class="vc-photo-body"><div class="vc-photo-stage"><img class="vc-photo-image" alt="" draggable="false" hidden><div class="vc-photo-status" role="status" aria-live="polite"></div></div><div class="vc-photo-details"><h3 data-vc-name></h3><dl><dt>Category</dt><dd data-vc-category>Not specified</dd><dt>Colour</dt><dd data-vc-colour>Not specified</dd></dl><div class="vc-photo-controls" role="group" aria-label="Photo zoom controls"><button class="vc-photo-button" type="button" data-vc-in aria-label="Zoom in">Zoom In (+)</button><button class="vc-photo-button" type="button" data-vc-out aria-label="Zoom out">Zoom Out (−)</button><button class="vc-photo-button" type="button" data-vc-reset>Reset Zoom</button><output class="vc-photo-zoom" aria-live="polite" aria-label="Zoom level">1×</output></div><p class="vc-photo-help">Use the zoom buttons, then drag the photo to look closer.</p></div></div></section>';
    document.body.append(dialog);
    const q = selector => dialog.querySelector(selector);
    stage = q('.vc-photo-stage'); photo = q('.vc-photo-image'); status = q('.vc-photo-status');
    title = q('[data-vc-name]'); category = q('[data-vc-category]'); colour = q('[data-vc-colour]');
    description = document.createElement('p');
    description.className = 'vc-photo-description'; description.hidden = true;
    title.after(description);
    zoomIn = q('[data-vc-in]'); zoomOut = q('[data-vc-out]'); reset = q('[data-vc-reset]');
    zoomLabel = q('.vc-photo-zoom'); closeButton = q('[data-vc-close]');
    closeButton.addEventListener('click', close);
    zoomIn.addEventListener('click', () => { scale = Math.min(4, scale + .5); paint(); });
    zoomOut.addEventListener('click', () => { scale = Math.max(1, scale - .5); paint(); });
    reset.addEventListener('click', () => { scale = 1; panX = panY = 0; paint(); });
    dialog.addEventListener('cancel', event => { event.preventDefault(); close(); });
    dialog.addEventListener('keydown', event => {
      if (event.key === 'Escape') { event.preventDefault(); event.stopPropagation(); close(); return; }
      if (event.key === 'Tab') {
        const buttons = [...dialog.querySelectorAll('button:not(:disabled)')];
        const first = buttons[0], last = buttons[buttons.length - 1];
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
      }
    });
    dialog.addEventListener('pointerdown', event => { backdropDown = event.target === dialog; });
    dialog.addEventListener('click', event => { if (backdropDown && event.target === dialog) close(); backdropDown = false; });
    dialog.addEventListener('wheel', event => { if (event.target === dialog || stage.contains(event.target)) event.preventDefault(); }, { passive: false });
    dialog.addEventListener('touchmove', event => { if (event.target === dialog) event.preventDefault(); }, { passive: false });
    stage.addEventListener('pointerdown', event => {
      if (!loaded || scale <= 1 || !event.isPrimary || event.button !== 0) return;
      event.preventDefault(); drag = { id: event.pointerId, x: event.clientX, y: event.clientY, panX, panY };
      stage.setPointerCapture(event.pointerId); stage.dataset.dragging = 'true';
    });
    stage.addEventListener('pointermove', event => {
      if (!drag || event.pointerId !== drag.id) return;
      panX = drag.panX + event.clientX - drag.x; panY = drag.panY + event.clientY - drag.y; paint();
    });
    const endDrag = () => { drag = null; stage.dataset.dragging = 'false'; };
    stage.addEventListener('pointerup', endDrag); stage.addEventListener('pointercancel', endDrag); stage.addEventListener('lostpointercapture', endDrag);
    new ResizeObserver(fit).observe(stage);
  }
  function domDetails(img) {
    const card = img.closest('article');
    const value = name => {
      const dt = card && [...card.querySelectorAll('dt')].find(el => el.textContent.trim() === name);
      return dt && dt.nextElementSibling ? dt.nextElementSibling.textContent.trim() : 'Not specified';
    };
    const paragraphs = card ? [...card.querySelectorAll('.catalog-info > p:not([class]), .product-info > p:not([class])')].filter(el => !el.querySelector('a,button,input')) : [];
    return { title: card?.querySelector('h2,h3')?.textContent.trim() || '', description: paragraphs.map(el => el.textContent.trim()).join(' '), category: value('Category'), colour: value('Colour'), alt: img.alt };
  }
  function details(record) {
    title.textContent = record.title || ''; title.hidden = !record.title;
    description.textContent = record.description || ''; description.hidden = !record.description;
    category.textContent = record.category || 'Not specified'; colour.textContent = record.colour || 'Not specified';
    photo.alt = record.alt || record.title || 'Product photo';
  }
  function open(img) {
    create(); trigger = img.closest('a') || img; const current = ++version;
    scale = 1; panX = panY = 0; fitWidth = fitHeight = 0; loaded = false; drag = null;
    stage.dataset.dragging = 'false'; photo.hidden = true; photo.removeAttribute('src');
    status.textContent = 'Loading photo…'; paint();
    const path = fullPath(relative(img.getAttribute('src')));
    const legacy = legacyNames[path.replace('assets/catalog/', '')];
    details(legacy ? { ...domDetails(img), title: legacy, category: 'Not specified', colour: 'Not specified', alt: legacy } : domDetails(img));
    lockScroll(); dialog.showModal(); closeButton.focus({ preventScroll: true });
    photo.onload = () => { if (version !== current || !dialog.open) return; loaded = true; photo.hidden = false; status.textContent = ''; fit(); };
    photo.onerror = () => { if (version !== current || !dialog.open) return; loaded = false; photo.hidden = true; status.textContent = 'This photo could not be loaded. Please close the popup and try again.'; paint(); };
    photo.src = absolute(path);
    if (!legacy) records().then(byImage => { if (version === current && dialog.open && byImage.has(path)) details(byImage.get(path)); });
  }
  document.addEventListener('click', event => {
    if (event.defaultPrevented || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
    const img = event.target instanceof Element && (event.target.matches('img') ? event.target : event.target.closest('a[data-vc-photo-trigger]')?.querySelector('img'));
    if (!img || !eligible(img) || typeof HTMLDialogElement === 'undefined' || !HTMLDialogElement.prototype.showModal) return;
    event.preventDefault(); open(img);
  });
  document.addEventListener('keydown', event => {
    if (event.ctrlKey || event.metaKey || event.altKey || event.shiftKey) return;
    const el = event.target;
    if (!(el instanceof Element) || !el.hasAttribute('data-vc-photo-trigger')) return;
    if (event.key === ' ' || (event.key === 'Enter' && el.matches('img'))) {
      const img = el.matches('img') ? el : el.querySelector('img');
      if (eligible(img) && typeof HTMLDialogElement !== 'undefined' && HTMLDialogElement.prototype.showModal) { event.preventDefault(); open(img); }
    }
  });
  discover(document.body);
  new MutationObserver(changes => changes.forEach(change => {
    if (change.type === 'attributes') prepare(change.target);
    else change.addedNodes.forEach(discover);
  })).observe(document.body, { childList: true, subtree: true, attributes: true, attributeFilter: ['src'] });
})();
