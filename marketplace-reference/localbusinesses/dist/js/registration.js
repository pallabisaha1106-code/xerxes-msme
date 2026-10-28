/* Static registration helper: prepares a WhatsApp message; it never uploads form data. */
const registrationForm = document.querySelector('#registration-form');
const categorySelect = registrationForm.elements.businessCategory;
// Add the Neighbourly administrator number with country code and digits only to open that chat directly.
const adminWhatsapp = '';
const categories = {
  Retailer: ['Ladies Wear','Men’s Wear','Kids Wear','Grocery','Electronics & Computers','Mobile & Accessories','Footwear','Home & Furniture','Beauty & Wellness','Jewellery','Pharmacy','Other'],
  Wholesaler: ['Textiles & Apparel','Grocery & Food','Electronics','Mobile & Accessories','Footwear','Home & Furniture','Beauty & Personal Care','Jewellery','Packaging & Supplies','Other']
};

function updateCategories() {
  const type = registrationForm.elements.businessKind.value;
  categorySelect.innerHTML = '<option value="">Select a category</option>' + categories[type].map(item => `<option>${item}</option>`).join('');
}

function clean(value) { return String(value || '').trim().replace(/[\r\n]+/g, ' '); }
function fileName(input) { return input.files[0] ? input.files[0].name : 'Not selected'; }
function applicationMessage() {
  const data = new FormData(registrationForm);
  return [
    'Hello! I would like to register my business on Neighbourly.', '',
    `Business type: ${clean(data.get('businessKind'))}`,
    `Business name: ${clean(data.get('businessName'))}`,
    `Business category: ${clean(data.get('businessCategory'))}`,
    `Owner name: ${clean(data.get('ownerName'))}`,
    `Email ID: ${clean(data.get('email'))}`,
    `Phone number: ${clean(data.get('phone'))}`,
    `Business WhatsApp: ${clean(data.get('businessWhatsapp'))}`, '',
    `Street / Landmark: ${clean(data.get('streetLandmark'))}`,
    `Area: ${clean(data.get('area'))}`,
    `Pincode: ${clean(data.get('pincode'))}`,
    `City: ${clean(data.get('city'))}`,
    `State: ${clean(data.get('state'))}`, '',
    'Documents selected (I will attach these in this chat):',
    `• Shop front photo: ${fileName(registrationForm.elements.shopPhoto)}`,
    `• Shop ACT/GST certificate: ${fileName(registrationForm.elements.businessDocument)}`,
    `• Owner PAN/Aadhaar: ${fileName(registrationForm.elements.ownerDocument)}`,
    '', 'Please review my business for verification. Thank you.'
  ].join('\n');
}
function openWhatsapp(message) {
  const recipient = /^\d{8,15}$/.test(adminWhatsapp) ? adminWhatsapp : '';
  window.open(`https://wa.me/${recipient}?text=${encodeURIComponent(message)}`, '_blank', 'noopener,noreferrer');
}

registrationForm.addEventListener('change', event => {
  if (event.target.name === 'businessKind') updateCategories();
  if (event.target.type === 'file') {
    const file = event.target.files[0];
    const display = event.target.closest('.upload-card').querySelector('.file-name');
    if (file && file.size > 8 * 1024 * 1024) {
      event.target.value = '';
      display.textContent = 'File is larger than 8 MB';
      display.classList.add('file-error');
    } else {
      display.textContent = file ? file.name : 'No file selected';
      display.classList.toggle('file-error', !file);
    }
  }
});

registrationForm.addEventListener('submit', async event => {
  event.preventDefault();
  const status = document.querySelector('#form-status');
  if (!registrationForm.checkValidity()) {
    registrationForm.reportValidity();
    status.textContent = 'Please complete all required fields before continuing.';
    return;
  }
  const message = applicationMessage();
  if (event.submitter?.value === 'whatsapp') {
    status.textContent = 'Opening WhatsApp. Please attach the three selected files before sending your message.';
    openWhatsapp(message);
    return;
  }
  const submitButton = event.submitter;
  submitButton.disabled = true;
  submitButton.textContent = 'Submitting…';
  status.textContent = '';
  try {
    const response = await fetch('/api/registrations', { method: 'POST', body: new FormData(registrationForm) });
    const result = await response.json();
    if (!response.ok) throw new Error(result.error || 'The application could not be submitted.');
    document.querySelector('.submission-reference').textContent = `Reference: ${result.reference}`;
    document.querySelector('#submission-confirmation').showModal();
    registrationForm.reset();
    updateCategories();
    registrationForm.querySelectorAll('.file-name').forEach(node => { node.textContent = 'No file selected'; node.classList.remove('file-error'); });
  } catch (error) {
    status.textContent = `${error.message} You can still submit using WhatsApp.`;
  } finally {
    submitButton.disabled = false;
    submitButton.innerHTML = '<span aria-hidden="true">✓</span> Submit form to review';
  }
});

const confirmationDialog = document.querySelector('#submission-confirmation');
confirmationDialog.querySelector('.dialog-close').addEventListener('click', () => confirmationDialog.close());
confirmationDialog.querySelector('.confirmation-close').addEventListener('click', () => confirmationDialog.close());

document.querySelector('.menu-button').addEventListener('click', () => {
  const navigation = document.querySelector('#navigation');
  const open = navigation.classList.toggle('open');
  document.querySelector('.menu-button').setAttribute('aria-expanded', String(open));
});
document.querySelector('#year').textContent = new Date().getFullYear();
if (new URLSearchParams(location.search).get('type') === 'wholesaler') {
  registrationForm.elements.businessKind.value = 'Wholesaler';
}
updateCategories();
