(() => {
  const menu = document.querySelector('.menu-button');
  const navigation = document.querySelector('#navigation');
  if (menu && navigation) {
    menu.addEventListener('click', () => {
      const open = navigation.classList.toggle('open');
      menu.setAttribute('aria-expanded', String(open));
    });
    navigation.addEventListener('click', event => {
      if (event.target.closest('a')) {
        navigation.classList.remove('open');
        menu.setAttribute('aria-expanded', 'false');
      }
    });
  }
  const year = document.querySelector('#year');
  if (year) year.textContent = String(new Date().getFullYear());
})();
