(function () {
  'use strict';

  function escapeHtml(value) {
    var div = document.createElement('div');
    div.textContent = value == null ? '' : String(value);
    return div.innerHTML;
  }

  function renderGrid(products) {
    var grid = document.getElementById('productGrid');
    var countEl = document.querySelector('[data-product-count]');
    if (!grid) return;

    if (countEl) {
      countEl.textContent = products.length + (products.length === 1 ? ' style' : ' styles');
    }
    if (products.length === 0) {
      grid.innerHTML = '<p>No products are available right now. Please check back soon.</p>';
      return;
    }

    grid.innerHTML = products.map(function (p) {
      var image = p.imageUrls && p.imageUrls[0];
      return '' +
        '<article class="product-card" data-variant-id="' + escapeHtml(p.variantId) + '">' +
        '<div class="product-card-image">' +
        (image ? '<img src="' + escapeHtml(image) + '" alt="' + escapeHtml(p.productName) + '" loading="lazy">' : '') +
        '</div>' +
        '<div class="product-card-body">' +
        '<h3>' + escapeHtml(p.productName) + '</h3>' +
        '<span class="product-price">&#8377;' + Number(p.sellingPrice).toFixed(2) + '</span>' +
        (p.inStock
          ? '<button type="button" class="btn primary" data-add-to-cart>Add to Cart</button>'
          : '<span class="product-out-of-stock">Currently out of stock</span>') +
        '</div>' +
        '</article>';
    }).join('');

    grid.querySelectorAll('[data-add-to-cart]').forEach(function (button) {
      button.addEventListener('click', function () {
        var variantId = button.closest('.product-card').dataset.variantId;
        var product = products.find(function (p) { return p.variantId === variantId; });
        if (product && window.XerxesCart) window.XerxesCart.add(product);
      });
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    fetch('data/products.json')
      .then(function (response) { return response.json(); })
      .then(renderGrid)
      .catch(function () {
        var grid = document.getElementById('productGrid');
        if (grid) grid.innerHTML = '<p>We could not load products right now. Please refresh the page.</p>';
      });
  });
})();
