const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const nodes=new Map();
function node(key){
 if(!nodes.has(key))nodes.set(key,{
  value:'',innerHTML:'',textContent:'',hidden:false,dataset:{},attributes:{},children:[],listeners:{},
  get options(){return this.children;},
  classList:{add(){},remove(){},toggle(){return true;}},
  append(child){this.children.push(child);},addEventListener(type,fn){this.listeners[type]=fn;},setAttribute(k,v){this.attributes[k]=v;},
  querySelector(){return node(key+' child');},querySelectorAll(){return [];},showModal(){},close(){},scrollIntoView(){}
 });
 return nodes.get(key);
}
const context=vm.createContext({window:{},document:{querySelector:node,querySelectorAll(){return [];},createElement(){return {value:'',textContent:''};},addEventListener(){}},location:{href:'http://localhost/'},URL,matchMedia(){return {matches:true};}});
vm.runInContext(fs.readFileSync('dist/data/businesses.js','utf8'),context);
vm.runInContext(fs.readFileSync('dist/data/pincode-areas.js','utf8'),context);
vm.runInContext(fs.readFileSync('dist/js/marketplace.js','utf8'),context);
const run=code=>vm.runInContext(code,context);
assert.ok(node('#area').children.some(option=>option.value==='Baner'),'all catalogue areas shown by default');
assert.ok(node('#pincode').children.some(option=>option.value==='410401'),'sample pincodes populate the combo');
run("applyPincode('411033',{renderResults:false})");
assert.equal(node('#pincode').value,'411033');assert.equal(node('#pincode-entry').value,'411033');
for(const locality of ['Chinchwadgaon','Thergaon','Tathawade','Punawale vicinity'])assert.ok(node('#area').children.some(option=>option.value===locality),locality);
node('#area').value='Punawale vicinity';
assert.equal(run("matches(businesses.find(b=>b.id==='mateshwari-super-mart'))"),true,'Poonawale alias matches the displayed locality');
assert.equal(run("matches(businesses.find(b=>b.id==='computer-town'))"),false,'other locality excluded');
run("applyPincode('',{renderResults:false})");assert.ok(node('#area').children.some(option=>option.value==='Koregaon Park'),'clearing pincode restores all areas');
for(const file of ['index.html','wholesalers.html']){
 const html=fs.readFileSync('dist/'+file,'utf8');
 assert.ok(html.includes('id="pincode-entry"'));assert.ok(html.includes('id="pincode-help"'));
 assert.ok(html.indexOf('data/pincode-areas.js')<html.indexOf('js/marketplace.js'));
 assert.ok(html.indexOf('id="pincode"')<html.indexOf('id="area"'));
}
console.log('PASS: typed/combo pincode synchronization, default areas, mapped localities, aliases, reset behavior and both marketplace integrations.');
