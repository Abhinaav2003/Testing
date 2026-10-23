/*
 * live-refresh.js
 * Checks the server once per second and reloads the page only when its content
 * has actually changed. Search text, sort choice, focus and scroll position are
 * saved before the reload and put back afterwards.
 *
 * Add to a page with:  <script th:src="@{/js/live-refresh.js}"></script>
 * Optional: <body data-live-interval="2000"> changes the interval (milliseconds).
 */
(function () {
    'use strict';

    var INTERVAL = parseInt(document.body.getAttribute('data-live-interval'), 10) || 1000;
    var KEY = 'liveRefresh:' + location.pathname + location.search;
    var baseline = null;
    var inFlight = false;

    /* ---------- restore state saved before the last auto reload ---------- */
    function restoreState() {
        var raw;
        try { raw = sessionStorage.getItem(KEY); sessionStorage.removeItem(KEY); } catch (e) { return; }
        if (!raw) return;
        var s;
        try { s = JSON.parse(raw); } catch (e) { return; }

        Object.keys(s.fields || {}).forEach(function (id) {
            var el = document.getElementById(id);
            if (!el) return;
            if (el.type === 'checkbox' || el.type === 'radio') {
                el.checked = s.fields[id];
            } else {
                el.value = s.fields[id];
            }
            // Lets the page's own search / sort code run again
            el.dispatchEvent(new Event('input', { bubbles: true }));
            el.dispatchEvent(new Event('change', { bubbles: true }));
        });

        if (s.focus) {
            var f = document.getElementById(s.focus);
            if (f) {
                f.focus({ preventScroll: true });
                try {
                    if (s.selStart !== null && s.selStart !== undefined) f.setSelectionRange(s.selStart, s.selEnd);
                } catch (e) { /* not every input type supports selection */ }
            }
        }
        window.scrollTo(0, s.scrollY || 0);
        window.addEventListener('load', function () { window.scrollTo(0, s.scrollY || 0); });
    }

    function saveState() {
        var fields = {};
        document.querySelectorAll('input[id], select[id], textarea[id]').forEach(function (el) {
            if (el.type === 'hidden' || el.type === 'password' || el.type === 'file') return;
            fields[el.id] = (el.type === 'checkbox' || el.type === 'radio') ? el.checked : el.value;
        });
        var a = document.activeElement;
        var state = {
            fields: fields,
            scrollY: window.scrollY,
            focus: a && a.id && fields.hasOwnProperty(a.id) ? a.id : null,
            selStart: null,
            selEnd: null
        };
        try { state.selStart = a.selectionStart; state.selEnd = a.selectionEnd; } catch (e) { /* ignore */ }
        try { sessionStorage.setItem(KEY, JSON.stringify(state)); } catch (e) { /* storage unavailable */ }
    }

    /* ---------- don't interrupt the user ---------- */
    function userIsBusy() {
        if (document.querySelector('.modal.show')) return true;          // modal open
        if (document.querySelector('.dropdown-menu.show')) return true;  // menu open
        var a = document.activeElement;
        if (a && (a.tagName === 'TEXTAREA' || a.type === 'password')) return true;
        var sel = window.getSelection && window.getSelection();
        if (sel && !sel.isCollapsed && String(sel).length > 0) return true; // text is selected
        return false;
    }

    /* ---------- polling ---------- */
    function check() {
        if (inFlight || document.hidden) return schedule();
        inFlight = true;

        fetch(location.href, {
            credentials: 'same-origin',
            cache: 'no-store',
            headers: { 'X-Live-Refresh': '1' }
        }).then(function (res) {
            // Session expired: server sent us to the login page
            if (res.redirected && new URL(res.url).pathname !== location.pathname) {
                location.reload();
                return null;
            }
            if (!res.ok) return null; // temporary server error: just try again
            return res.text();
        }).then(function (html) {
            if (html === null || html === undefined) return;
            if (baseline === null) { baseline = html; return; }
            if (html !== baseline && !userIsBusy()) {
                saveState();
                location.reload();
            }
        }).catch(function () {
            /* offline or server restarting: try again next tick */
        }).then(function () {
            inFlight = false;
            schedule();
        });
    }

    function schedule() { setTimeout(check, INTERVAL); }

    document.addEventListener('visibilitychange', function () {
        if (!document.hidden && !inFlight) check();
    });

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', restoreState);
    } else {
        restoreState();
    }
    check();
})();
