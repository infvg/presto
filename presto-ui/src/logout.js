// Need a separate file to work around CSP
window.addEventListener('load', () => {
    fetch('/v1/ui/logout', {
        method: 'GET'
    }).catch(() => {
        // Silently swallow the error
    });
});