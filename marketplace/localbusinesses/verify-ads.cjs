
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
class Element{
 constructor(){this.children=[];this.listeners={};this.attrs={};this.hidden=false;}
 append(...items){this.children.push(...items);}
 setAttribute(k,v){this.attrs[k]=v;}
 addEventListener(k,v){this.listeners[k]=v;}
 contains(value){return this===value||this.children.some(c=>c.contains(value));}
}
function setup(slides,reduced=false){
 const elements=new Map(),get=k=>{if(!elements.has(k))elements.set(k,new Element());return elements.get(k);};
 const root=get('#top-advertisement');root.querySelector=get;
 const motion={matches:reduced,addEventListener(){}},doc={hidden:false,querySelector:get,createElement:()=>new Element(),addEventListener(){}};
 let callback=null,delay;
 const context={window:{},document:doc,location:{href:'http://localhost/'},URL,matchMedia:()=>motion,setInterval(fn,ms){callback=fn;delay=ms;return 1;},clearInterval(){callback=null;}};
 vm.createContext(context);vm.runInContext(fs.readFileSync('dist/data/advertisements.js','utf8'),context);
 if(slides!==undefined)context.window.marketplaceAds.slides=slides;
 vm.runInContext(fs.readFileSync('dist/js/advertisements.js','utf8'),context);
 return {get,root,delay:()=>delay,tick:()=>callback?.(),running:()=>!!callback,data:context.window.marketplaceAds};
}
const app=setup(),panels=app.get('.ad-slides').children;
assert.equal(panels.length,4);assert.deepEqual(panels.map(p=>p.hidden),[false,true,true,true]);
assert.equal(panels[0].className,"banner-slide banner-image-only");assert.equal(panels[0].children.length,1);assert.equal(panels[0].href,undefined);
app.tick();assert.deepEqual(panels.map(p=>p.hidden),[true,false,true,true]);
app.tick();assert.equal(panels[2].hidden,false);
app.tick();assert.equal(panels[3].hidden,false);
app.tick();assert.equal(panels[0].hidden,false);
assert.equal(app.delay(),3000);
assert.equal(app.root.listeners.mouseenter,undefined);
assert.equal(app.running(),true);
app.root.listeners.focusin();assert.equal(app.running(),false);
const reduced=setup(undefined,true);assert.equal(reduced.running(),true);reduced.tick();assert.equal(reduced.get('.ad-slides').children[1].hidden,false);
assert.equal(setup([]).root.hidden,true);
assert.equal(setup([app.data.slides[0]]).running(),false);
const bad=setup([{image:'images/veera-anarkali.png',shopName:'Test',url:'javascript:alert(1)'}]);
assert.equal(bad.get('.ad-slides').children[0].href,undefined);
for(const slide of app.data.slides)assert.ok(fs.existsSync('dist/'+slide.image));
for(const panel of panels.slice(1)){assert.equal(panel.href,'https://www.veeracollections.com/');assert.equal(panel.rel,'noopener noreferrer');}
const html=fs.readFileSync('dist/index.html','utf8');
assert.ok(!/ad-controls|ad-previous|ad-next|ad-pause|ad-dots/.test(html));
assert.ok(html.indexOf('id="top-advertisement"')<html.indexOf('class="discovery-strip"'));
assert.ok(html.indexOf('class="discovery-strip"')<html.indexOf('id="categories"'));
assert.equal((html.match(/id="search"/g)||[]).length,1);
console.log('PASS: 3-second autoplay, wraparound, no visible controls, no hover pause, focus pause, autoplay with reduced motion, empty/single ads, safe destinations, local assets and section order.');

