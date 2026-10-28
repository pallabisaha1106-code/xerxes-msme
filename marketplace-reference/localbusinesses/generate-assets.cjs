const fs = require('node:fs');
const path = require('node:path');
const out = path.join(__dirname,'dist');
const svg = (body,w=400,h=250) => `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}">${body}</svg>`;
const write = (name,body) => fs.writeFileSync(path.join(out,'images',`${name}.svg`),body);
const rows = [
 ['veera','Veera Collection','Ladies Wear','Gujar Nagar','411033','Shop no 12, Upper ground, ABC Nirman Building, Dange Chowk Rd','#b9776f','#f2dfd6','Dresses','Kurtas','Sarees','fashion'],
 ['daily','The Daily Basket','Grocery','Aundh','411007','8, ITI Road, opposite Aundh Post Office','#668a58','#e5ead9','Fresh vegetables','Seasonal fruit','Pantry essentials','food'],
 ['clay','Clay & Co.','Home & Kitchen','Koregaon Park','411001','24, North Main Road, Lane 5','#b3875d','#eee2d1','Ceramic mugs','Serving bowls','Planters','home'],
 ['sole','Sole Stories','Footwear','Kothrud','411038','16, Paud Road, near Anand Nagar','#778b92','#e1e9e9','Everyday sneakers','Leather sandals','Casual shoes','shoe'],
 ['tara','Tara Jewellery','Jewellery','Baner','411045','32, Baner–Pashan Link Road','#b79857','#f4ebd5','Gold-tone earrings','Everyday necklaces','Handcrafted bangles','jewel'],
 ['bloom','Bloom Beauty','Beauty & Cosmetics','Viman Nagar','411014','7, Phoenix Road, Sakore Nagar','#a57991','#f1e1e9','Skincare essentials','Lip colours','Natural soaps','beauty'],
 ['little','Little Sprouts','Kids Wear','Aundh','411007','21, DP Road, near Parihar Chowk','#a5a066','#efecd5','Cotton tops','Playtime dresses','Baby essentials','fashion'],
 ['circuit','Circuit Corner','Electronics','Kothrud','411038','5, Karve Road, near Vanaz Corner','#657c89','#dde6eb','Headphones','Smart speakers','Accessories','tech'],
 ['linen','Linen & Loom','Men’s Wear','Koregaon Park','411001','19, Lane 6, North Main Road','#84967e','#e7ecdf','Linen shirts','Cotton trousers','Everyday tees','fashion'],
 ['mango','Mango & Mint','Restaurants','Viman Nagar','411014','11, Datta Mandir Road','#c4985c','#f6ebd6','Fresh salads','Artisan coffee','Weekend brunch','food'],
 ['mobile','Pocket Tech','Mobile Shops','Baner','411045','42, Balewadi High Street','#7d87ab','#e5e7f2','Mobile phones','Phone cases','Chargers','tech'],
 ['wood','Woodfolk Studio','Furniture','Kothrud','411038','31, DP Road, Mayur Colony','#9b7957','#ecdfd1','Accent chairs','Side tables','Wooden shelves','home']
];
function shop(name,color,bg,offset=0){return svg(`<rect width="400" height="250" fill="${bg}"/><circle cx="340" cy="39" r="24" fill="#fff" opacity=".35"/><path d="M0 221H400V250H0Z" fill="#c7b9a2" opacity=".45"/><rect x="47" y="45" width="306" height="178" rx="3" fill="#faf6e9"/><rect x="59" y="56" width="282" height="33" rx="2" fill="${color}"/><text x="200" y="77" text-anchor="middle" font-family="Arial,sans-serif" font-weight="bold" font-size="14" letter-spacing="2" fill="#fff">${name.toUpperCase().replace(/&/g,'&amp;')}</text><rect x="65" y="111" width="175" height="101" fill="#344d47"/><rect x="255" y="111" width="77" height="112" fill="#40554b"/><rect x="264" y="121" width="59" height="65" fill="#cbd6c4"/><path d="M73 120h157v7H73zM149 111v101" stroke="#e7e0ce" stroke-width="4"/><rect x="268" y="196" width="8" height="3" rx="1" fill="#d6bb71"/><path d="M46 89h308l10 27H36Z" fill="${color}"/>${Array.from({length:9},(_,i)=>`<path d="M${49+i*34} 89h17l5 27h-23Z" fill="#f8edda"/><path d="M${38+i*36} 116h18v8a9 9 0 0 1-18 0" fill="${i%2?color:'#f8edda'}"/>`).join('')}<path d="M81 198h141" stroke="#dfc9a5" stroke-width="5"/>${Array.from({length:5},(_,i)=>`<path d="M${87+i*27} 158l7-9 7 9v35h-14z" fill="${['#cea681','#ba7770','#d1c295','#95a28b','#e3d8be'][(i+offset)%5]}"/><path d="M${94+i*27} 149v-8" stroke="#cabfaa" stroke-width="2"/>`).join('')}<rect x="27" y="195" width="25" height="31" rx="2" fill="#b18765"/><ellipse cx="40" cy="173" rx="20" ry="28" fill="#738862"/><path d="M40 164v33" stroke="#536b45" stroke-width="3"/><rect x="350" y="201" width="22" height="26" fill="#ac8062"/><ellipse cx="361" cy="184" rx="15" ry="24" fill="#87986b"/><path d="M58 225h282" stroke="#b0a18a" stroke-width="4"/>`);}
function product(type,index,color,bg){let art='';
if(type==='fashion') art=`<path d="M43 34L29 48l13 18 11-8v52h55V58l11 8 13-18-16-14-20-7c-3 15-21 15-26 0Z" fill="${color}"/><path d="M69 28c5 18 23 18 28 0M80 48v61" fill="none" stroke="#fff" opacity=".4" stroke-width="2"/>`;
if(type==='food') art=`<ellipse cx="80" cy="83" rx="57" ry="33" fill="#fcf9ed"/><ellipse cx="80" cy="80" rx="45" ry="24" fill="#d9dfbd"/>${Array.from({length:8},(_,i)=>`<circle cx="${48+(i%4)*20}" cy="${64+Math.floor(i/4)*22}" r="12" fill="${['#819b52','#d79450','#c66f51'][((i+index)%3)]}"/>`).join('')}`;
if(type==='home') art=`<path d="M38 57h59v32c0 32-59 32-59 0Z" fill="${color}"/><path d="M98 64h8c23 0 20 30-8 30" fill="none" stroke="${color}" stroke-width="9"/><ellipse cx="68" cy="57" rx="29" ry="7" fill="#e6d3b6"/><ellipse cx="68" cy="57" rx="22" ry="3" fill="#7d6551"/>`;
if(type==='shoe') art=`<path d="M30 90l9-43 24 12 22 18 37 9c13 4 19 13 13 20H32Z" fill="${color}"/><path d="M31 106h103" stroke="#fff8e9" stroke-width="10"/><path d="M68 67l-8 11m19-5-7 10m18-6-6 10" stroke="#f8f2e6" stroke-width="4"/>`;
if(type==='jewel') art=`<path d="M45 38c-10 69 83 91 71 0" fill="none" stroke="#b4944a" stroke-width="5"/><path d="M68 99l12-14 13 14-13 17Z" fill="#bb9a51"/><circle cx="80" cy="100" r="6" fill="#e8d6a6"/>`;
if(type==='beauty') art=`<rect x="42" y="53" width="40" height="59" rx="7" fill="${color}"/><rect x="50" y="34" width="24" height="21" rx="3" fill="#d4b595"/><rect x="46" y="72" width="32" height="24" fill="#faf5e6"/><path d="M94 60h22l5 52H89Z" fill="#c4a686"/><rect x="94" y="49" width="22" height="13" fill="#726c60"/>`;
if(type==='tech') art=`<rect x="48" y="25" width="63" height="95" rx="9" fill="#3c484c"/><rect x="53" y="32" width="53" height="76" rx="4" fill="${color}"/><path d="M53 82l53-40v66H53Z" fill="#fff" opacity=".18"/><circle cx="80" cy="114" r="3" fill="#a2b1b3"/>`;
return svg(`<rect width="160" height="140" fill="${bg}"/><ellipse cx="80" cy="119" rx="48" ry="5" fill="#38402e" opacity=".07"/>${art}`,160,140);}
const veeraLinks = {
  "instagram": "https://www.instagram.com/veera.collectionss",
  "facebook": "https://www.facebook.com/share/18ES1ZSB3Z/",
  "youtube": "https://www.youtube.com/@veeracollectionss",
  "googleMaps": "https://maps.app.goo.gl/ducwauewX8tGmd2Z6",
  "whatsapp": "https://chat.whatsapp.com/IpsTwctzlrhH2lqOZne2RV?s=cl&p=i&ilr=0&amv=0",
  "website": "https://www.veeracollections.com/"
};
const businesses = rows.map((r,i)=>{ const [id,businessName,businessType,area,pincode,street,color,bg,...rest]=r; const names=rest.slice(0,3),type=rest[3]; write(id,shop(businessName,color,bg,i)); const products=names.map((name,j)=>{write(`${id}-${j}`,product(type,j,[color,'#ad9478','#839887'][j],[bg,'#eee9df','#e6eadd'][j]));return {name,image:`images/${id}-${j}.svg`};});return {id,businessName,businessType,shopPhoto:`images/${id}.svg`,address:`${street}, Pune`,area,pincode,links:id==='veera' ? {...veeraLinks} : {instagram:'',facebook:'',youtube:'',whatsapp:'',website:'',googleMaps:`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(area+', Pune '+pincode)}`},products};});
Object.assign(businesses.find(b => b.id === "veera"), {
 "enquiryWhatsapp": "919921959748",
 "productPage": {
  "enabled": true,
  "url": "veera-collection.html",
  "products": [
    {
      "id": "VC-IW-01",
      "name": "Purple floral set",
      "category": "Indo-western",
      "image": "images/veera-products/indo-western-01.png"
    },
    {
      "id": "VC-IW-02",
      "name": "Plum jacket set",
      "category": "Indo-western",
      "image": "images/veera-products/indo-western-02.png"
    },
    {
      "id": "VC-IW-03",
      "name": "Yellow floral set",
      "category": "Indo-western",
      "image": "images/veera-products/indo-western-03.png"
    },
    {
      "id": "VC-IW-04",
      "name": "Blue printed jacket dress",
      "category": "Indo-western",
      "image": "images/veera-products/indo-western-04.png"
    },
    {
      "id": "VC-IW-05",
      "name": "Pink floral dress with cream jacket",
      "category": "Indo-western",
      "image": "images/veera-products/indo-western-05.png"
    },
    {
      "id": "VC-IW-06",
      "name": "Blue layered dress",
      "category": "Indo-western",
      "image": "images/veera-products/indo-western-06.png"
    },
    {
      "id": "VC-IW-07",
      "name": "Light green printed dress",
      "category": "Indo-western",
      "image": "images/veera-products/indo-western-07.png"
    },
    {
      "id": "VC-IW-08",
      "name": "Multicolour printed jacket set",
      "category": "Indo-western",
      "image": "images/veera-products/indo-western-08.png"
    },
    {
      "id": "VC-IW-09",
      "name": "Brown floral dress",
      "category": "Indo-western",
      "image": "images/veera-products/indo-western-09.png"
    }
  ]
},
  "address": "Shop no 12, Upper ground, ABC Nirman Building, Dange Chowk Rd, Gujar Nagar, 411033",
  "shopPhoto": "images/veera-shop.png",
  "shopPhotoAlt": "Veera Collection storefront with shop sign and clothing displays",
  "products": [
    {
      "name": "Anarkali Dresses",
      "image": "images/veera-anarkali.png"
    },
    {
      "name": "Sharara Sets",
      "image": "images/veera-sharara.png"
    },
    {
      "name": "Western Wear",
      "image": "images/veera-western.png"
    }
  ]
});
businesses.splice(1, 0, {
  "id": "computer-town",
  "businessName": "Computer Town",
  "businessType": "Computer/Laptop shop",
  "shopPhoto": "images/computer-town-shop.jpeg",
  "shopPhotoAlt": "Computer Town storefront with computer and laptop displays",
  "address": "Thergaon, Pimpri-Chinchwad, Pune – 411033",
  "area": "Thergaon",
  "pincode": "411033",
  "products": [
    {
      "name": "Dell desktop computer",
      "image": "images/computer-town-product-1.jpeg"
    },
    {
      "name": "Dell laptop",
      "image": "images/computer-town-product-2.jpeg"
    },
    {
      "name": "Lenovo ThinkPad laptop",
      "image": "images/computer-town-product-3.jpeg"
    }
  ],
  "links": {
    "instagram": "https://www.instagram.com/",
    "facebook": "",
    "youtube": "",
    "googleMaps": "https://share.google/6mMu2JAYvA0pUnekw",
    "whatsapp": "https://whatsapp.com/channel/0029VaAkgS70rGiGtiJD4D07",
    "website": "https://www.computertownstore.com/"
  }
});
// Keep this shop sample consistent with the live listing; images are maintained in its shop folder.
businesses.splice(2, 0, {
  "id": "mateshwari-super-mart",
  "businessName": "Krishna Mart",
  "businessType": "Grocery",
  "shopPhoto": "images/mateshwari-super-mart/krishna-mart-front.jpeg",
  "shopPhotoAlt": "Krishna Mart storefront",
  "address": "Poonawale",
  "area": "Poonawale",
  "city": "Pune",
  "pincode": "411033",
  "products": [
    {
      "id": "MSM-001",
      "name": "Aashirvaad Atta",
      "packSize": "5 kg",
      "mrp": 279,
      "category": "Grocery",
      "image": "images/mateshwari-super-mart/product-01.jpg"
    },
    {
      "id": "MSM-002",
      "name": "India Gate Basmati Rice",
      "packSize": "1 kg",
      "mrp": 189,
      "category": "Grocery",
      "image": "images/mateshwari-super-mart/product-02.jpg"
    },
    {
      "id": "MSM-003",
      "name": "Tata Salt",
      "packSize": "1 kg",
      "mrp": 22,
      "category": "Grocery",
      "image": "images/mateshwari-super-mart/product-03.jpg"
    }
  ],
  "links": {
    "instagram": "",
    "facebook": "",
    "youtube": "",
    "googleMaps": "https://maps.app.goo.gl/DPejmeNAb5a2m9jB8?g_st=ac",
    "whatsapp": "",
    "website": ""
  },
  "enquiryWhatsapp": "917666069942",
  "productPage": {
    "enabled": true,
    "url": "mateshwari-super-mart.html",
    "products": [
      {
        "id": "MSM-004",
        "name": "Tata Sugar",
        "packSize": "1 kg",
        "mrp": 44,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-04.jpg"
      },
      {
        "id": "MSM-005",
        "name": "Fortune Sunflower Oil",
        "packSize": "1 L",
        "mrp": 165,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-05.jpg"
      },
      {
        "id": "MSM-006",
        "name": "Amul Ghee",
        "packSize": "1 L",
        "mrp": 610,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-06.jpg"
      },
      {
        "id": "MSM-007",
        "name": "Mother Dairy Milk",
        "packSize": "1 L",
        "mrp": 60,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-07.jpg"
      },
      {
        "id": "MSM-008",
        "name": "Red Label Tea",
        "packSize": "250 g",
        "mrp": 125,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-08.jpg"
      },
      {
        "id": "MSM-009",
        "name": "Nescafé Coffee",
        "packSize": "100 g",
        "mrp": 280,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-09.jpg"
      },
      {
        "id": "MSM-010",
        "name": "Haldiram’s Soan Papdi",
        "packSize": "250 g",
        "mrp": 175,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-10.jpg"
      },
      {
        "id": "MSM-011",
        "name": "Moong Dal",
        "packSize": "1 kg",
        "mrp": 120,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-11.jpg"
      },
      {
        "id": "MSM-012",
        "name": "Masoor Dal",
        "packSize": "1 kg",
        "mrp": 110,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-12.jpg"
      },
      {
        "id": "MSM-013",
        "name": "Toor Dal",
        "packSize": "1 kg",
        "mrp": 135,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-13.jpg"
      },
      {
        "id": "MSM-014",
        "name": "Chana Dal",
        "packSize": "1 kg",
        "mrp": 115,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-14.jpg"
      },
      {
        "id": "MSM-015",
        "name": "Urad Dal",
        "packSize": "1 kg",
        "mrp": 130,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-15.jpg"
      },
      {
        "id": "MSM-016",
        "name": "Rajma",
        "packSize": "1 kg",
        "mrp": 150,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-16.jpg"
      },
      {
        "id": "MSM-017",
        "name": "Chana",
        "packSize": "1 kg",
        "mrp": 90,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-17.jpg"
      },
      {
        "id": "MSM-018",
        "name": "Kabuli Chana",
        "packSize": "1 kg",
        "mrp": 120,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-18.jpg"
      },
      {
        "id": "MSM-019",
        "name": "Masala (MDH Garam)",
        "packSize": "100 g",
        "mrp": 75,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-19.jpg"
      },
      {
        "id": "MSM-020",
        "name": "Turmeric Powder",
        "packSize": "100 g",
        "mrp": 40,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-20.jpg"
      },
      {
        "id": "MSM-021",
        "name": "Red Chilli Powder",
        "packSize": "100 g",
        "mrp": 45,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-21.jpg"
      },
      {
        "id": "MSM-022",
        "name": "Coriander Powder",
        "packSize": "100 g",
        "mrp": 40,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-22.jpg"
      },
      {
        "id": "MSM-023",
        "name": "Cumin Seeds",
        "packSize": "100 g",
        "mrp": 70,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-23.jpg"
      },
      {
        "id": "MSM-024",
        "name": "Mustard Seeds",
        "packSize": "100 g",
        "mrp": 35,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-24.jpg"
      },
      {
        "id": "MSM-025",
        "name": "Jeera Powder",
        "packSize": "100 g",
        "mrp": 55,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-25.jpg"
      },
      {
        "id": "MSM-026",
        "name": "Kitchen King Masala",
        "packSize": "100 g",
        "mrp": 60,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-26.jpg"
      },
      {
        "id": "MSM-027",
        "name": "Maggi Noodles",
        "packSize": "70 g",
        "mrp": 14,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-27.jpg"
      },
      {
        "id": "MSM-028",
        "name": "Patanjali Honey",
        "packSize": "250 g",
        "mrp": 160,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-28.jpg"
      },
      {
        "id": "MSM-029",
        "name": "Kissan Tomato Ketchup",
        "packSize": "500 g",
        "mrp": 95,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-29.jpg"
      },
      {
        "id": "MSM-030",
        "name": "Ching's Schezwan Sauce",
        "packSize": "250 g",
        "mrp": 95,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-30.jpg"
      },
      {
        "id": "MSM-031",
        "name": "Britannia Marie Gold",
        "packSize": "250 g",
        "mrp": 30,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-31.jpg"
      },
      {
        "id": "MSM-032",
        "name": "Parle-G Biscuits",
        "packSize": "250 g",
        "mrp": 25,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-32.jpg"
      },
      {
        "id": "MSM-033",
        "name": "Good Day Cookies",
        "packSize": "200 g",
        "mrp": 30,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-33.jpg"
      },
      {
        "id": "MSM-034",
        "name": "Tata Sampann Poha",
        "packSize": "500 g",
        "mrp": 45,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-34.jpg"
      },
      {
        "id": "MSM-035",
        "name": "Sooji (Rava)",
        "packSize": "500 g",
        "mrp": 40,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-35.jpg"
      },
      {
        "id": "MSM-036",
        "name": "Besan",
        "packSize": "1 kg",
        "mrp": 70,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-36.jpg"
      },
      {
        "id": "MSM-037",
        "name": "Corn Flour",
        "packSize": "100 g",
        "mrp": 35,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-37.jpg"
      },
      {
        "id": "MSM-038",
        "name": "Coconut Oil",
        "packSize": "500 ml",
        "mrp": 160,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-38.jpg"
      },
      {
        "id": "MSM-039",
        "name": "Vim Dishwash Bar",
        "packSize": "300 g",
        "mrp": 30,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-39.jpg"
      },
      {
        "id": "MSM-040",
        "name": "Surf Excel Detergent",
        "packSize": "1 kg",
        "mrp": 145,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-40.jpg"
      },
      {
        "id": "MSM-041",
        "name": "Harpic Toilet Cleaner",
        "packSize": "500 ml",
        "mrp": 110,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-41.jpg"
      },
      {
        "id": "MSM-042",
        "name": "Dettol Antiseptic Liquid",
        "packSize": "500 ml",
        "mrp": 170,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-42.jpg"
      },
      {
        "id": "MSM-043",
        "name": "Colgate Toothpaste",
        "packSize": "200 g",
        "mrp": 95,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-43.jpg"
      },
      {
        "id": "MSM-044",
        "name": "Dove Bath Soap",
        "packSize": "100 g",
        "mrp": 55,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-44.jpg"
      },
      {
        "id": "MSM-045",
        "name": "Himalaya Shampoo",
        "packSize": "400 ml",
        "mrp": 150,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-45.jpg"
      },
      {
        "id": "MSM-046",
        "name": "Santoor Soap",
        "packSize": "100 g",
        "mrp": 45,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-46.jpg"
      },
      {
        "id": "MSM-047",
        "name": "Pears Soap",
        "packSize": "125 g",
        "mrp": 65,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-47.jpg"
      },
      {
        "id": "MSM-048",
        "name": "Tide Detergent Powder",
        "packSize": "1 kg",
        "mrp": 145,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-48.jpg"
      },
      {
        "id": "MSM-049",
        "name": "Air Freshener",
        "packSize": "300 ml",
        "mrp": 85,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-49.jpg"
      },
      {
        "id": "MSM-050",
        "name": "Eveready LED Bulb",
        "packSize": "9W",
        "mrp": 70,
        "category": "Grocery",
        "image": "images/mateshwari-super-mart/product-50.jpg"
      }
    ]
  }
});
const categories=[['Ladies Wear','♧'],['Men’s Wear','♧'],['Kids Wear','☆'],['Footwear','♧'],['Jewellery','◇'],['Beauty & Cosmetics','✿'],['Electronics','▣'],['Computer/Laptop shop','▣'],['Mobile Shops','▯'],['Grocery','♧'],['Home & Kitchen','⌂'],['Furniture','⊞'],['Restaurants','♨'],['Services','⚙'],['Other','⋯']].map(([name,icon])=>({name,icon}));
fs.writeFileSync(path.join(out,'data','businesses.js'),'// Sample businesses: replace details, images and map links before launch.\n// Shop links: full HTTP(S) URLs. Empty or missing links hide their buttons.\nwindow.marketplaceData = '+JSON.stringify({config:{name:'Neighbourly',adminWhatsapp:'',defaultCity:'Pune',alwaysShowDiscovery:true,welcomeCategories:['Grocery','Restaurants','Ladies Wear','Beauty & Cosmetics']},categories,businesses},null,2)+';\n');
write('placeholder',svg('<rect width="400" height="250" fill="#edf0e7"/><path d="M145 115l55-40 55 40v75H145Z" fill="#c2d0b9"/><rect x="186" y="140" width="28" height="50" fill="#82977a"/><text x="200" y="224" text-anchor="middle" font-family="Arial" font-size="12" fill="#566d4d">A local find · Photo coming soon</text>'));
write('mark',svg('<rect width="40" height="40" rx="11" fill="#236747"/><text x="10" y="31" font-family="Arial" font-weight="bold" font-size="33" fill="white">n</text><circle cx="29" cy="10" r="3" fill="#e7c968"/>',40,40));
write('neighbourhood',svg(`<rect width="520" height="310" fill="#f4f5eb"/><circle cx="385" cy="70" r="34" fill="#e8cd7d"/><path d="M40 256c115-27 315-21 453 0v22H40Z" fill="#e0e4d4"/><path d="M77 249V110h119v139" fill="#d7b092"/><path d="M86 102h100V78H86Z" fill="#e4c8ab"/><path d="M194 249V90h140v159" fill="#e8cf9e"/><path d="M207 90V69h113v21" fill="#e8cf9e"/><path d="M335 249V125h107v124" fill="#b1bca4"/><path d="M347 125V107h83v18" fill="#b1bca4"/><rect x="94" y="174" width="83" height="70" fill="#465e50"/><rect x="211" y="160" width="101" height="84" fill="#547366"/><rect x="351" y="180" width="74" height="64" fill="#4f6153"/><path d="M133 175v69m129-83v83m126-63v63" stroke="#e4ddbf" stroke-width="5"/><rect x="84" y="133" width="105" height="27" fill="#b77160"/><rect x="202" y="117" width="123" height="27" fill="#557554"/><rect x="344" y="145" width="90" height="22" fill="#e9e0c0"/><text x="136" y="151" text-anchor="middle" font-family="Arial" font-size="11" font-weight="bold" fill="#fff2dd">THE CORNER</text><text x="263" y="135" text-anchor="middle" font-family="Arial" font-size="11" font-weight="bold" fill="#fff2dd">GOOD THINGS</text><text x="389" y="160" text-anchor="middle" font-family="Arial" font-size="10" font-weight="bold" fill="#54664a">DAILY GOODS</text><path d="M81 160h111l8 19H72Z" fill="#e3b7a1"/><path d="M201 144h126l9 20H193Z" fill="#789270"/><path d="M342 167h95l8 17H335Z" fill="#e5d9b2"/>${[82,107,132,157,182].map(x=>`<path d="M${x} 160h12l4 19h-17Z" fill="#faf0d8"/>`).join('')}${[201,228,255,282,309].map(x=>`<path d="M${x} 144h13l4 20h-19Z" fill="#f3e5c2"/>`).join('')}<rect x="63" y="231" width="23" height="24" fill="#b78f6c"/><ellipse cx="74" cy="203" rx="20" ry="30" fill="#839267"/><rect x="437" y="230" width="22" height="25" fill="#b78f6c"/><ellipse cx="448" cy="201" rx="23" ry="31" fill="#738963"/><path d="M448 204v31" stroke="#59714e" stroke-width="3"/><path d="M67 252h394" stroke="#aeb798" stroke-width="3"/><rect x="287" y="193" width="16" height="21" fill="#e3c384"/><rect x="217" y="196" width="18" height="24" fill="#c99875"/><path d="M215 223h34m21 0h33" stroke="#dfd9b7" stroke-width="3"/><path d="M37 109l7 7 7-7M465 88l7 7 7-7" stroke="#b4bea7" fill="none" stroke-width="2"/>`,520,310));
console.log(`Created ${businesses.length} businesses and 51 original SVG illustrations.`);
