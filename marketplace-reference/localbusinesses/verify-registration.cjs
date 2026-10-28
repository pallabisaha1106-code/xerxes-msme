const fs = require('node:fs');
const assert = require('node:assert/strict');

const html = fs.readFileSync('dist/register.html', 'utf8');
const js = fs.readFileSync('dist/js/registration.js', 'utf8');
const css = fs.readFileSync('dist/css/style.css', 'utf8');

for (const name of ['businessKind','businessName','businessCategory','phone','businessWhatsapp','ownerName','email','streetLandmark','area','pincode','city','state','shopPhoto','businessDocument','ownerDocument','consent']) {
  assert.ok(html.includes(`name="${name}"`), `missing ${name}`);
}
for (const field of ['shopPhoto','businessDocument','ownerDocument']) {
  assert.match(html, new RegExp(`name="${field}"[^>]*required`), `${field} must be required`);
}
assert.ok(html.includes('Shop ACT or GST certificate'));
assert.ok(html.includes('Owner PAN or Aadhaar'));
assert.ok(html.includes('application/pdf'));
assert.ok(html.includes('href="index.html"') && html.includes('href="wholesalers.html"'));
assert.ok(js.includes('https://wa.me/${recipient}?text='));
assert.ok(html.includes('value="review"') && html.includes('Submit form to review'));
assert.ok(html.includes('value="whatsapp"') && html.includes('Review &amp; continue on WhatsApp'));
assert.ok(html.includes('id="submission-confirmation"') && js.includes("event.submitter?.value === 'whatsapp'"));
assert.ok(js.includes("fetch('/api/registrations'") && js.includes("method: 'POST'"));
for (const label of ['Business type:', 'Business name:', 'Business category:', 'Owner name:', 'Email ID:', 'Phone number:', 'Business WhatsApp:', 'Street / Landmark:', 'Area:', 'Pincode:', 'City:', 'State:']) assert.ok(js.includes(label), `WhatsApp message missing ${label}`);
assert.ok(!html.includes('name="ownerWhatsapp"'));
assert.ok(!html.includes('name="address"'));
assert.ok(js.includes('Shop ACT/GST certificate:') && js.includes('Owner PAN/Aadhaar:'));
assert.ok(js.includes("get('type') === 'wholesaler'"));
assert.ok(css.includes('.registration-shell') && css.includes('@media(max-width:600px)'));
console.log('PASS: retailer/wholesaler registration fields; required uploads; validation; responsive styles; complete WhatsApp handoff.');
