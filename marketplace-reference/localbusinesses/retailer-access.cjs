const path=require('node:path');const {createRetailerStore}=require('./retailer-store.cjs');
if(!process.argv[2]){console.error('Usage: node retailer-access.cjs SHOP_ID');process.exit(1);}
const store=createRetailerStore(path.join(__dirname,'submissions/retailers.sqlite'),path.join(__dirname,'dist/data/businesses.js'));
try{console.log('Retailer access key (share privately): '+store.issueKey(process.argv[2]));}finally{store.close();}
