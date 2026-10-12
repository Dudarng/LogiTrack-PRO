(function () {
  var KEY = 'logitrack-theme';
  var root = document.documentElement;

  function readStored() {
    try {
      var v = localStorage.getItem(KEY);
      return v === 'light' || v === 'dark' ? v : null;
    } catch (e) {
      return null;
    }
  }

  function writeStored(v) {
    try {
      localStorage.setItem(KEY, v);
    } catch (e) {}
  }

  function systemTheme() {
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
  }

  var DEFAULT_THEME = 'dark';

  function apply(theme) {
    root.setAttribute('data-theme', theme);
    var buttons = document.querySelectorAll('[data-theme-toggle]');
    for (var i = 0; i < buttons.length; i++) {
      buttons[i].setAttribute('aria-pressed', theme === 'dark' ? 'true' : 'false');
    }
  }

  apply(readStored() || DEFAULT_THEME);

  document.addEventListener('DOMContentLoaded', function () {
    apply(root.getAttribute('data-theme'));
  });

  document.addEventListener('click', function (e) {
    var btn = e.target.closest ? e.target.closest('[data-theme-toggle]') : null;
    if (!btn) return;
    var next = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
    root.classList.add('theme-anim');
    apply(next);
    writeStored(next);
    window.setTimeout(function () { root.classList.remove('theme-anim'); }, 300);
  });

  window.addEventListener('storage', function (e) {
    if (e.key === KEY && (e.newValue === 'light' || e.newValue === 'dark')) apply(e.newValue);
  });
})();
