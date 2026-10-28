(function () {
  'use strict';

  var config = window.XERXES_STOREFRONT || {};
  var storageKey = 'xerxes_storefront_cart_' + (config.tenantId || 'default');
  var drawer, body, countEls;
  var lastSubmitSucceeded = false;

  function readCart() {
    try {
      var raw = localStorage.getItem(storageKey);
      return raw ? JSON.parse(raw) : [];
    } catch (e) {
      return [];
    }
  }

  function writeCart(items) {
    try {
      localStorage.setItem(storageKey, JSON.stringify(items));
    } catch (e) {
      /* private-browsing or storage-full: cart just won't persist across reloads */
    }
    updateCount(items);
  }

  function updateCount(items) {
    var count = items.reduce(function (sum, item) { return sum + item.quantity; }, 0);
    (countEls || document.querySelectorAll('[data-cart-count]')).forEach(function (el) {
      el.textContent = String(count);
    });
  }

  function add(product) {
    var items = readCart();
    var existing = items.find(function (i) { return i.variantId === product.variantId; });
    if (existing) {
      existing.quantity += 1;
    } else {
      items.push({
        variantId: product.variantId,
        productName: product.productName,
        sku: product.sku,
        price: product.sellingPrice,
        imageUrl: (product.imageUrls && product.imageUrls[0]) || '',
        quantity: 1,
      });
    }
    writeCart(items);
    render();
    open();
  }

  function setQuantity(variantId, quantity) {
    var items = readCart();
    if (quantity <= 0) {
      items = items.filter(function (i) { return i.variantId !== variantId; });
    } else {
      var item = items.find(function (i) { return i.variantId === variantId; });
      if (item) item.quantity = quantity;
    }
    writeCart(items);
    render();
  }

  function escapeHtml(value) {
    var div = document.createElement('div');
    div.textContent = value == null ? '' : String(value);
    return div.innerHTML;
  }

  function render() {
    if (!body) return;
    var items = readCart();
    if (lastSubmitSucceeded) {
      body.innerHTML = '<div class="checkout-success"><p>Thank you! Your order request has been sent. We will contact you shortly to confirm details.</p></div>';
      return;
    }
    if (items.length === 0) {
      body.innerHTML = '<p class="cart-empty">Your cart is empty.</p>';
      return;
    }
    var subtotal = items.reduce(function (sum, i) { return sum + i.price * i.quantity; }, 0);
    var lines = items.map(function (item) {
      return '' +
        '<div class="cart-line" data-variant-id="' + escapeHtml(item.variantId) + '">' +
        (item.imageUrl ? '<img src="' + escapeHtml(item.imageUrl) + '" alt="">' : '') +
        '<div class="cart-line-info">' +
        '<div>' + escapeHtml(item.productName) + '</div>' +
        '<div class="cart-qty">' +
        '<button type="button" data-qty-decrease>&minus;</button>' +
        '<span>' + item.quantity + '</span>' +
        '<button type="button" data-qty-increase>+</button>' +
        '</div>' +
        '<button type="button" class="cart-line-remove" data-remove>Remove</button>' +
        '</div>' +
        '<div>&#8377;' + (item.price * item.quantity).toFixed(2) + '</div>' +
        '</div>';
    }).join('');

    body.innerHTML = lines +
      '<div class="cart-summary"><span>Subtotal</span><span>&#8377;' + subtotal.toFixed(2) + '</span></div>' +
      '<form class="checkout-form" id="checkoutForm">' +
      '<label for="cf-name">Name</label><input id="cf-name" name="customerName" required>' +
      '<label for="cf-phone">Phone</label><input id="cf-phone" name="customerPhone" required>' +
      '<label for="cf-email">Email (optional)</label><input id="cf-email" name="customerEmail" type="email">' +
      '<label for="cf-address1">Address</label><input id="cf-address1" name="addressLine1">' +
      '<label for="cf-city">City</label><input id="cf-city" name="city">' +
      '<label for="cf-pincode">Pincode</label><input id="cf-pincode" name="pincode">' +
      '<label for="cf-notes">Notes (optional)</label><textarea id="cf-notes" name="notes" rows="2"></textarea>' +
      '<div class="checkout-error" id="checkoutError" hidden></div>' +
      '<button type="submit" class="btn primary">Place Order Request</button>' +
      '</form>';

    body.querySelectorAll('[data-qty-increase]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var variantId = btn.closest('.cart-line').dataset.variantId;
        var item = items.find(function (i) { return i.variantId === variantId; });
        setQuantity(variantId, item.quantity + 1);
      });
    });
    body.querySelectorAll('[data-qty-decrease]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var variantId = btn.closest('.cart-line').dataset.variantId;
        var item = items.find(function (i) { return i.variantId === variantId; });
        setQuantity(variantId, item.quantity - 1);
      });
    });
    body.querySelectorAll('[data-remove]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var variantId = btn.closest('.cart-line').dataset.variantId;
        setQuantity(variantId, 0);
      });
    });
    var form = document.getElementById('checkoutForm');
    if (form) form.addEventListener('submit', submitOrder);
  }

  function submitOrder(event) {
    event.preventDefault();
    var form = event.target;
    var errorEl = document.getElementById('checkoutError');
    errorEl.hidden = true;
    var items = readCart();
    var payload = {
      customerName: form.customerName.value,
      customerPhone: form.customerPhone.value,
      customerEmail: form.customerEmail.value || null,
      addressLine1: form.addressLine1.value || null,
      city: form.city.value || null,
      pincode: form.pincode.value || null,
      notes: form.notes.value || null,
      lineItems: items.map(function (i) {
        return { variantId: i.variantId, quantity: i.quantity };
      }),
    };
    var submitBtn = form.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    fetch(config.apiBaseUrl + '/api/public/storefront/' + config.tenantId + '/orders', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }).then(function (response) {
      if (!response.ok) throw new Error('Request failed');
      return response.json();
    }).then(function () {
      lastSubmitSucceeded = true;
      writeCart([]);
      render();
    }).catch(function () {
      errorEl.textContent = 'Could not submit your order. Please check your details and try again, or contact us directly.';
      errorEl.hidden = false;
      submitBtn.disabled = false;
    });
  }

  function open() {
    if (!drawer) return;
    drawer.classList.add('is-open');
    drawer.setAttribute('aria-hidden', 'false');
    document.body.classList.add('cart-open');
  }

  function close() {
    if (!drawer) return;
    drawer.classList.remove('is-open');
    drawer.setAttribute('aria-hidden', 'true');
    document.body.classList.remove('cart-open');
  }

  document.addEventListener('DOMContentLoaded', function () {
    drawer = document.getElementById('cartDrawer');
    body = document.getElementById('cartBody');
    countEls = document.querySelectorAll('[data-cart-count]');
    updateCount(readCart());
    render();
    document.querySelectorAll('[data-cart-toggle]').forEach(function (btn) {
      btn.addEventListener('click', open);
    });
    document.querySelectorAll('[data-cart-close]').forEach(function (btn) {
      btn.addEventListener('click', close);
    });
    drawer.addEventListener('click', function (event) {
      if (event.target === drawer) close();
    });
    document.addEventListener('keydown', function (event) {
      if (event.key === 'Escape') close();
    });
  });

  window.XerxesCart = { add: add, open: open, close: close };
})();
