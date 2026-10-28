(function () {
  'use strict';

  // Hides links/blocks whose configured value ended up empty (e.g. no
  // WhatsApp number, no map link configured) rather than showing a dead link.
  function pruneOptionalElements() {
    document.querySelectorAll('[data-optional-link]').forEach(function (el) {
      var href = el.getAttribute('href') || '';
      var bare = href.replace(/^(tel:|mailto:|https:\/\/wa\.me\/)/, '').trim();
      if (!bare) el.remove();
    });
    document.querySelectorAll('[data-optional-block]').forEach(function (el) {
      if (!el.textContent.trim()) el.remove();
    });
  }

  function initNav() {
    var nav = document.getElementById('primary-nav');
    var toggle = document.querySelector('.menu-toggle');
    if (!nav || !toggle) return;
    function close() {
      nav.classList.remove('is-open');
      toggle.setAttribute('aria-expanded', 'false');
      document.body.classList.remove('menu-open');
    }
    toggle.addEventListener('click', function () {
      var open = nav.classList.toggle('is-open');
      toggle.setAttribute('aria-expanded', String(open));
      document.body.classList.toggle('menu-open', open);
    });
    nav.querySelectorAll('a').forEach(function (a) {
      a.addEventListener('click', close);
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    pruneOptionalElements();
    initNav();
  });
})();
