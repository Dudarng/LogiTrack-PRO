(function () {
  function initDemoLogin() {
    var btn = document.getElementById('btnDemoLogin');
    if (!btn) return;

    btn.addEventListener('click', function () {
      var u = document.getElementById('u');
      var s = document.getElementById('s');
      if (u && s) {
        u.value = btn.getAttribute('data-user') || '';
        s.value = btn.getAttribute('data-pass') || '';
        u.dispatchEvent(new Event('input', { bubbles: true }));
        s.dispatchEvent(new Event('input', { bubbles: true }));
        var submitBtn = document.querySelector('form button[type="submit"]');
        if (submitBtn) {
          submitBtn.focus();
        }
      }
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initDemoLogin);
  } else {
    initDemoLogin();
  }
})();
