try {
  const nonce = "cHJlc3RvLXVpCg==";
  const originalCreateElement = document.createElement;
  document.createElement = function(tagName, ...args) {
      const el = originalCreateElement.call(document, tagName, ...args);
      if (tagName.toLowerCase() === 'style') {
          el.setAttribute('nonce', nonce);
          el.nonce = nonce;
      }
      return el;
  };

  if (typeof document.createStyleSheet === 'function') {
      const originalCreateStyleSheet = document.createStyleSheet;
      document.createStyleSheet = function() {
          const sheet = originalCreateStyleSheet.apply(this, arguments);
          try {
          // IE-only: add nonce if possible (ignored in modern browsers)
          if (sheet.owningElement) {
              sheet.owningElement.setAttribute('nonce', nonce);
          }
          } catch (e) {}
          return sheet;
      };
  }

  const originalAppendChild = document.head.appendChild;
  document.head.appendChild = function (el) {
      if (el.tagName === 'STYLE' && nonce) {
      el.setAttribute('nonce', nonce);
      }
      return originalAppendChild.call(document.head, el);
  };
} catch(err) {
  console.error('Error inject CSP helper functions', err);
}
