import fs from 'node:fs/promises';
import {Workbook, SpreadsheetFile} from '@oai/artifact-tool';
const out='E:/localbusinesses/outputs/01a09b35-retailer-plan';
const wb=Workbook.create();
const names=['Start here','Packages','Monthly plan','Package trials'];
const ss=Object.fromEntries(names.map(n=>[n,wb.worksheets.add(n)]));
const money='"₹"#,##0;[Red]("₹"#,##0);"₹"0';
function val(s,a,v){s.getRange(a).values=v;}
function f(s,a,v){s.getRange(a).formulas=[[v]];}
function header(s,a){s.getRange(a).format={fill:'#253C50',font:{bold:true,color:'#FFFFFF'},wrapText:true,rowHeight:36};}
function input(s,a){s.getRange(a).format.fill='#FFF2CC';s.getRange(a).format.font.color='#1756A9';}
function base(s,title,subtitle,last,rows){s.showGridLines=false;s.getRange(`A1:${last}${rows}`).format.font={name:'Arial',size:11,color:'#243447'};s.getRange(`A1:${last}${rows}`).format.rowHeight=25;s.getRange(`A1:${last}${rows}`).format.columnWidth=18;val(s,'A1',[[title]]);s.getRange('A1').format.font={size:17,bold:true};val(s,'A2',[[subtitle]]);s.getRange('A2').format.font={size:10,color:'#627487'};}
const p=ss.Packages;
base(p,'Retailer packages','Pilot assumptions in INR. Yellow cells are editable. Costs include delivery time.','I',16);
val(p,'A4:I9',[
 ['Package','Service scope','Setup fee','Monthly fee','Setup hours','Monthly hours','Setup cash cost','Monthly cash cost','Retailer fit'],
 ['Free listing','Shop details and contact links',0,0,0.25,0.05,0,0,'Directory entry'],
 ['Starter catalogue','20 products; 1 update batch/month',1999,499,2,0.5,100,25,'Small shops'],
 ['Standard catalogue','50 products; 2 update batches/month',2999,699,3,1,150,50,'Grocery and clothing'],
 ['Managed catalogue','100 products; 4 update batches/month',4999,999,5,2,250,75,'Frequently changing stock'],
 ['Own website','Separate website; 50 products; 2 update batches/month',9999,999,12,1.5,1000,150,'Own brand website']
]);header(p,'A4:I4');input(p,'A5:I9');p.getRange('A:A').format.columnWidth=25;p.getRange('B:B').format.columnWidth=39;p.getRange('I:I').format.columnWidth=25;p.getRange('A5:I9').format.wrapText=true;p.getRange('A5:I9').format.rowHeight=56;p.getRange('C5:D9').setNumberFormat(money);p.getRange('G5:H9').setNumberFormat(money);p.getRange('E5:F9').setNumberFormat('0.00');
val(p,'A12', [['Scope notes']]);
val(p,'A13', [['One update batch means one consolidated list of changes supplied by the retailer. Agree photo and product limits before selling.']]);
val(p,'A14', [['Monthly cash costs exclude labour and central overhead. Own website costs assume hosting/domain allowances; replace with actual quotes.']]);
val(p,'A15', [['No sales guarantee. Sponsored ads are optional and charged separately. Prices and workload estimates are unvalidated pilot assumptions.']]);
const m=ss['Monthly plan'];base(m,'Monthly business plan','One representative month. Existing retailers exclude new sign-ups; all new sign-ups pay a full first month.','I',39);
val(m,'A4:I4',[['Package','Existing retailers','New sign-ups','Billed retailers','Monthly revenue','Setup revenue','Service hours','Direct cash costs','Contribution']]);header(m,'A4:I4');
for(let r=5;r<=9;r++){f(m,`A${r}`,`='Packages'!A${r}`);val(m,`B${r}:C${r}`,[[[10,5,20,3,0][r-5],[2,1,3,1,0][r-5]]]);f(m,`D${r}`,`=B${r}+C${r}`);f(m,`E${r}`,`=D${r}*'Packages'!D${r}`);f(m,`F${r}`,`=C${r}*'Packages'!C${r}`);f(m,`G${r}`,`=D${r}*'Packages'!F${r}+C${r}*'Packages'!E${r}`);f(m,`H${r}`,`=D${r}*'Packages'!H${r}+C${r}*'Packages'!G${r}`);f(m,`I${r}`,`=E${r}+F${r}-H${r}-G${r}*$B$15`);}
val(m,'A10',[['Total']]);for(const c of 'BCDEFGHI') f(m,`${c}10`,`=SUM(${c}5:${c}9)`);input(m,'B5:C9');m.getRange('A:A').format.columnWidth=29;m.getRange('B:I').format.columnWidth=19;m.getRange('A5:A9').format.wrapText=true;m.getRange('A5:I9').format.rowHeight=36;m.getRange('E5:F10').setNumberFormat(money);m.getRange('H5:I10').setNumberFormat(money);m.getRange('G5:G10').setNumberFormat('0.0');
val(m,'A13:C22',[
 ['Shared cost inputs','Value','Definition'],
 ['Available hours/month',100,'Total hours available for service delivery and sales/admin'],
 ['Labour cost/hour',250,'Value of your time or paid delivery labour; avoid double counting'],
 ['Hosting and tools/month',1000,'Central costs only; per-shop costs are on Packages'],
 ['Marketing/month',2000,'Promotion and customer acquisition spending'],
 ['Travel and admin/month',1000,'Cash costs, excluding labour'],
 ['Other fixed costs/month',0,'Other cash overhead'],
 ['Sales/admin hours/month',10,'Non-delivery time costed at the same hourly rate'],
 ['Ad units sold/month',0,'One unit is one advertiser placement-month'],
 ['Price per ad unit',1000,'Charge only after demonstrating relevant traffic']
]);header(m,'A13:C13');input(m,'B14:B22');m.getRange('A14:A22').format.wrapText=true;m.getRange('A14:C22').format.rowHeight=34;m.getRange('C14:C22').format.columnWidth=25;m.getRange('C14:C22').format.wrapText=true;
val(m,'E13:F13',[['Monthly results','INR / hours']]);header(m,'E13:F13');m.getRange('E:E').format.columnWidth=29;
const results=[['Recurring revenue','=E10+B21*B22'],['One-time setup revenue','=F10'],['Total revenue','=F14+F15'],['Direct cash costs','=H10'],['Labour cost','=(G10+B20)*B15'],['Fixed cash overhead','=SUM(B16:B19)'],['Profit before tax','=F16-SUM(F17:F19)'],['Profit margin','=IF(F16=0,"N/A",F20/F16)'],['Total hours required','=G10+B20'],['Spare capacity hours','=B14-F22'],['Recurring-only profit',"=F14-SUMPRODUCT(D5:D9,'Packages'!H5:H9)-(SUMPRODUCT(D5:D9,'Packages'!F5:F9)+B20)*B15-F19"]];
results.forEach(([l,x],i)=>{val(m,`E${14+i}`,[[l]]);f(m,`F${14+i}`,x);});m.getRange('F14:F24').setNumberFormat(money);m.getRange('F21').setNumberFormat('0.0%');m.getRange('F22:F23').setNumberFormat('0.0');m.getRange('E14:E24').format.wrapText=true;m.getRange('E14:F24').format.rowHeight=34;
val(m,'A26',[['Reading the results']]);val(m,'A27',[['Contribution deducts package cash costs and service labour. Profit also deducts central overhead and sales/admin labour.']]);val(m,'A28',[['Recurring-only profit excludes setup revenue and setup costs, but includes the current billed retailer base and advertising.']]);val(m,'A29',[['Negative spare capacity means the plan needs fewer clients, narrower service scope or more available hours.']]);val(m,'A30',[['Prices exclude tax. This is a monthly operating model, not a cash-flow forecast; no churn, tax, debt or payment timing is modelled.']]);
const t=ss['Package trials'];base(t,'Compare retailer offers','Each trial is a separate month using one paid package. Yellow inputs do not alter the Monthly plan.','D',35);t.getRange('A:A').format.columnWidth=39;t.getRange('B:D').format.columnWidth=27;
val(t,'A4:D4',[['Trial assumptions','Trial A','Trial B','Trial C']]);header(t,'A4:D4');
const labels=['Package','Setup price override','Monthly price override','Existing retailers','New sign-ups','Setup hours / retailer','Monthly hours / retailer','Setup cash / retailer','Monthly cash / retailer','Billed retailers','Recurring revenue','Setup revenue','Total revenue','Direct cash costs','Labour cost','Fixed overhead','Profit before tax','Recurring-only profit','Total hours required','Spare capacity hours','Monthly contribution / retailer','Retailers to cover fixed costs'];labels.forEach((x,i)=>val(t,`A${i+5}`,[[x]]));
val(t,'B5:D9',[['Starter catalogue','Standard catalogue','Managed catalogue'],[1999,2999,4999],[499,699,999],[5,20,10],[1,3,2]]);input(t,'B5:D9');t.getRange('B5:D5').dataValidation={rule:{type:'list',formula1:"'Packages'!$A$6:$A$9"}};
for(const c of 'BCD'){
 for(const [r,col] of [[10,'E'],[11,'F'],[12,'G'],[13,'H']])f(t,`${c}${r}`,`=INDEX('Packages'!$${col}$5:$${col}$9,MATCH(${c}5,'Packages'!$A$5:$A$9,0))`);
 const forms={14:`${c}8+${c}9`,15:`${c}14*${c}7`,16:`${c}9*${c}6`,17:`${c}15+${c}16`,18:`${c}9*${c}12+${c}14*${c}13`,19:`${c}23*'Monthly plan'!$B$15`,20:"SUM('Monthly plan'!$B$16:$B$19)",21:`${c}17-SUM(${c}18:${c}20)`,22:`${c}15-${c}14*${c}13-(${c}14*${c}11+'Monthly plan'!$B$20)*'Monthly plan'!$B$15-${c}20`,23:`${c}9*${c}10+${c}14*${c}11+'Monthly plan'!$B$20`,24:`'Monthly plan'!$B$14-${c}23`,25:`${c}7-${c}13-${c}11*'Monthly plan'!$B$15`,26:`IF(${c}25<=0,"No break-even",ROUNDUP((${c}20+'Monthly plan'!$B$20*'Monthly plan'!$B$15)/${c}25,0))`};for(const [r,x] of Object.entries(forms))f(t,`${c}${r}`,`=${x}`);
}
t.getRange('B6:D7').setNumberFormat(money);t.getRange('B12:D13').setNumberFormat(money);t.getRange('B15:D22').setNumberFormat(money);t.getRange('B25:D25').setNumberFormat(money);t.getRange('B10:D11').setNumberFormat('0.00');t.getRange('B23:D24').setNumberFormat('0.0');t.getRange('A5:D26').format.rowHeight=28;
val(t,'A29',[['How to test']]);val(t,'A30',[['Select a package, then enter the setup and monthly prices you want to test. Override prices remain editable when the package changes.']]);val(t,'A31',[['Hours and direct cash costs follow Packages. Shared overhead, hourly labour cost and available hours follow Monthly plan.']]);val(t,'A32',[['Break-even uses recurring fees only. It excludes setup income and advertising. Trials exclude free listings and all add-ons.']]);
const s=ss['Start here'];base(s,'Neighbourly business plan','Retailer package experiments | INR | Prepared 13 September 2026','C',29);s.getRange('A:A').format.columnWidth=27;s.getRange('B:B').format.columnWidth=66;s.getRange('C:C').format.columnWidth=46;
val(s,'A4:C8',[
 ['Start with','Action','Where'],
 ['1. Define your offers','Edit prices, product limits, update batches and delivery costs.','Packages'],
 ['2. Build a sales mix','Enter existing retailers, new sign-ups, overhead and available hours.','Monthly plan'],
 ['3. Compare price tests','Try three independent combinations of package, price and retailer volume.','Package trials'],
 ['4. Run a pilot','Pitch five local shops; record enquiries, delivery hours and first-month renewals.','Use actual results to replace the assumptions']
]);header(s,'A4:C4');
val(s,'A11:C18',[
 ['Income stream','Pilot offer / suggested pricing','How to use it'],
 ['Catalogue services','Setup ₹2,000–₹5,000; monthly management ₹499–₹999.','Main recurring offer; modelled in Packages'],
 ['Advertising','₹500–₹2,000 per advertiser-month.','Enter units and price in Monthly plan; measure traffic first'],
 ['Own website','₹9,999 setup; define ongoing support and hosting separately.','Included as a package; replace delivery cost estimates'],
 ['Wholesaler promotion','₹1,000–₹3,000/month for catalogue promotion.','Future offer after attracting retailer buyers; not in totals'],
 ['Seasonal campaigns','₹1,000–₹3,000 per festival campaign.','Future add-on; budget artwork and promotion costs separately'],
 ['Qualified enquiries','Agree pricing and valid-lead rules with each business.','Later; requires enquiry tracking; not in totals'],
 ['Sales commission','Do not assume commission revenue from WhatsApp clicks.','Current website does not confirm completed sales']
]);header(s,'A11:C11');
val(s,'A21:C26',[
 ['Basis and limitations','Context','Source'],
 ['Pilot assumptions','All prices, sales volumes, hours and costs are editable proposals, not market benchmarks or promised earnings.','Business plan discussed in this conversation'],
 ['Website capabilities','Directory, product pages, advertisements and WhatsApp enquiries; no completed-order payment tracking.','Neighbourly workspace README.md'],
 ['Competitive context','Google Business Profile is free. Charge for catalogue work, maintenance and measurable promotion.','https://business.google.com/in/business-profile/'],
 ['Colour key','Yellow cells with blue text are inputs. Other numeric results are formulas.','Keep formula cells intact when testing packages'],
 ['Model scope','One full-billing month, before tax. Existing and new retailers are counted once; no automatic churn or annual forecast.','Labour values owner time; profit is after that allowance']
]);header(s,'A21:C21');s.getRange('A4:C26').format.wrapText=true;for(const [a,b] of [[5,8],[12,18],[22,26]])s.getRange(`A${a}:C${b}`).format.rowHeight=57;
for(const sh of [p,m,t]){sh.freezePanes.freezeRows(4);}
for(const [sh,rng] of [[m,'I5:I10'],[m,'F20'],[m,'F23:F24'],[t,'B21:D22'],[t,'B24:D25']])sh.getRange(rng).conditionalFormats.add('cellIs',{operator:'lessThan',formula:0,format:{font:{color:'#B42318'},fill:'#FEE4E2'}});
for(const [sh,ranges] of [[m,['B5:C9','B21']],[t,['B8:D9']]])for(const range of ranges)sh.dataValidations.add({range,rule:{type:'whole',operator:'greaterThanOrEqual',formula1:0}});
for(const [sh,rg] of [[m,'A10:I10'],[m,'E20:F20'],[t,'A21:D21']]){sh.getRange(rg).format.fill='#E7EEF4';sh.getRange(rg).format.font.bold=true;}
for(const sh of Object.values(ss))sh.getUsedRange().format.verticalAlignment='center';
m.getRange('A14:I22').format.rowHeight=57;
m.getRange('B15:B19').setNumberFormat(money);m.getRange('B22').setNumberFormat(money);
console.log((await wb.inspect({kind:'table',range:"'Monthly plan'!E14:F24",include:'values,formulas',tableMaxRows:12,tableMaxCols:2,maxChars:4000})).ndjson);
// Perturb one trial price and check profit responds, then restore.
const original=t.getRange('B21').values[0][0];val(t,'B7',[[599]]);const changed=t.getRange('B21').values[0][0];if(Math.abs((changed-original)-600)>0.01)throw Error(`Price sensitivity mismatch: ${original} -> ${changed}`);val(t,'B7',[[499]]);
console.log((await wb.inspect({kind:'match',searchTerm:'#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A|#NUM!|#NULL!',options:{useRegex:true,maxResults:30},summary:'Formula error scan',maxChars:2000})).ndjson);
for(const [name,range] of [['Start here','A1:C26'],['Packages','A1:I9'],['Monthly plan','A1:I24'],['Package trials','A1:D26']]){const img=await wb.render({sheetName:name,range,scale:1,format:'png'});await fs.writeFile(`${out}/${name.replaceAll(' ','-')}.png`,new Uint8Array(await img.arrayBuffer()));}
await (await SpreadsheetFile.exportXlsx(wb)).save(`${out}/Neighbourly-Retailer-Business-Plan.xlsx`);
console.log('Saved workbook');
