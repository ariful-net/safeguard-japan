/* =========================================================
   SafeGuard Japan — core.js
   全画面共通: 多言語文言、言語切替、通信ヘルパー、位置情報、
   トースト通知、オンライン/オフライン表示、Service Worker 登録
   ========================================================= */
(function () {
  'use strict';

  const SG = window.SG || (window.SG = { lang: 'ja', i18n: {} });
  const LANGS = ['ja', 'en', 'bn', 'vi'];
  const LANG_KEY = 'sg_lang';

  /* ---------- 文言 ---------- */
  SG.t = function (key, ...args) {
    let s = SG.i18n[key];
    if (s == null || s === '') return key;
    args.forEach((a, i) => { s = s.replace('{' + i + '}', a); });
    return s;
  };

  /* ---------- localStorage（失敗しても動くように） ---------- */
  SG.store = {
    get(key, fallback = null) {
      try { const v = localStorage.getItem(key); return v == null ? fallback : JSON.parse(v); }
      catch (e) { return fallback; }
    },
    set(key, value) {
      try { localStorage.setItem(key, JSON.stringify(value)); return true; }
      catch (e) { return false; }
    },
    remove(key) { try { localStorage.removeItem(key); } catch (e) { /* noop */ } }
  };

  /* ---------- 言語切替（正本は LocalStorage。サーバー描画用に Cookie にも書く） ---------- */
  function writeLangCookie(lang) {
    document.cookie = 'sg_lang=' + lang + '; path=/; max-age=31536000; SameSite=Lax';
  }

  SG.setLang = function (lang) {
    if (!LANGS.includes(lang)) lang = 'ja';
    SG.store.set(LANG_KEY, lang);
    writeLangCookie(lang);
    const url = new URL(location.href);
    url.searchParams.set('lang', lang);
    location.href = url.toString();
  };

  // 画面表示時に保存済み言語を反映（URL に lang が無いときだけ）
  (function syncLang() {
    const saved = SG.store.get(LANG_KEY);
    const url = new URL(location.href);
    if (url.searchParams.has('lang')) {
      // URL で明示された言語を保存しておく
      SG.store.set(LANG_KEY, SG.lang);
      writeLangCookie(SG.lang);
      return;
    }
    if (saved && LANGS.includes(saved) && saved !== SG.lang) {
      writeLangCookie(saved);
      url.searchParams.set('lang', saved);
      location.replace(url.toString());
    }
  })();

  /* ---------- 通信（タイムアウト付き） ---------- */
  SG.fetchJSON = async function (url, { timeout = 8000 } = {}) {
    const ctrl = new AbortController();
    const timer = setTimeout(() => ctrl.abort(), timeout);
    try {
      const res = await fetch(url, { signal: ctrl.signal, headers: { Accept: 'application/json' } });
      const body = await res.json().catch(() => null);
      if (!res.ok) {
        const err = new Error('HTTP ' + res.status);
        err.status = res.status; err.body = body;
        throw err;
      }
      return body;
    } finally {
      clearTimeout(timer);
    }
  };

  SG.withLang = function (path, params = {}) {
    const u = new URL(path, location.origin);
    Object.entries(params).forEach(([k, v]) => { if (v != null && v !== '') u.searchParams.set(k, v); });
    u.searchParams.set('lang', SG.lang);
    return u.pathname + u.search;
  };

  /* ---------- 位置情報 ---------- */
  // 位置はメモリと sessionStorage（タブを閉じると消える）にだけ置く。サーバーには保存しない。
  SG.getPosition = function ({ timeout = 10000, maxAge = 60000 } = {}) {
    return new Promise((resolve) => {
      const fallback = (reason) => resolve({ ...SG.defaultLocation, isDefault: true, reason });
      if (!('geolocation' in navigator)) return fallback('unsupported');
      navigator.geolocation.getCurrentPosition(
        (p) => {
          const pos = { lat: p.coords.latitude, lon: p.coords.longitude, accuracy: p.coords.accuracy, isDefault: false };
          try { sessionStorage.setItem('sg_pos', JSON.stringify({ lat: pos.lat, lon: pos.lon, at: Date.now() })); } catch (e) { /* noop */ }
          resolve(pos);
        },
        (err) => fallback(err.code === 1 ? 'denied' : 'unavailable'),
        { enableHighAccuracy: true, timeout, maximumAge: maxAge }
      );
    });
  };

  SG.lastPosition = function (maxAgeMs = 5 * 60 * 1000) {
    try {
      const p = JSON.parse(sessionStorage.getItem('sg_pos') || 'null');
      if (p && Date.now() - p.at < maxAgeMs) return { lat: p.lat, lon: p.lon, isDefault: false };
    } catch (e) { /* noop */ }
    return null;
  };

  /* ---------- 距離・方角 ---------- */
  SG.distance = function (a, b) {
    const R = 6371000, toR = (d) => d * Math.PI / 180;
    const dLat = toR(b.lat - a.lat), dLon = toR(b.lon - a.lon);
    const h = Math.sin(dLat / 2) ** 2 + Math.cos(toR(a.lat)) * Math.cos(toR(b.lat)) * Math.sin(dLon / 2) ** 2;
    return 2 * R * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
  };
  SG.bearing = function (a, b) {
    const toR = (d) => d * Math.PI / 180, toD = (r) => r * 180 / Math.PI;
    const y = Math.sin(toR(b.lon - a.lon)) * Math.cos(toR(b.lat));
    const x = Math.cos(toR(a.lat)) * Math.sin(toR(b.lat)) - Math.sin(toR(a.lat)) * Math.cos(toR(b.lat)) * Math.cos(toR(b.lon - a.lon));
    return (toD(Math.atan2(y, x)) + 360) % 360;
  };
  SG.compass = function (deg) {
    const dirs = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'];
    return SG.t('dir.' + dirs[Math.round(deg / 45) % 8]);
  };
  SG.formatDistance = function (m) {
    return m >= 1000 ? (m / 1000).toFixed(m >= 10000 ? 0 : 1) + ' km' : Math.round(m) + ' m';
  };
  SG.formatTime = function (iso, opts = { hour: '2-digit', minute: '2-digit' }) {
    if (!iso) return '';
    const d = new Date(iso);
    if (isNaN(d)) return iso;
    const loc = { ja: 'ja-JP', en: 'en-US', bn: 'bn-BD', vi: 'vi-VN' }[SG.lang] || 'ja-JP';
    return d.toLocaleString(loc, { timeZone: 'Asia/Tokyo', ...opts });
  };

  /* ---------- DOM ヘルパー（テキストは textContent で入れる = XSS 対策） ---------- */
  SG.el = function (tag, attrs = {}, ...children) {
    const node = document.createElement(tag);
    Object.entries(attrs).forEach(([k, v]) => {
      if (v == null || v === false) return;
      if (k === 'class') node.className = v;
      else if (k === 'text') node.textContent = v;
      else if (k.startsWith('on') && typeof v === 'function') node.addEventListener(k.slice(2), v);
      else node.setAttribute(k, v === true ? '' : v);
    });
    children.flat().forEach((c) => { if (c != null) node.append(c instanceof Node ? c : document.createTextNode(String(c))); });
    return node;
  };

  /* ---------- トースト ---------- */
  SG.toast = function (msg, type = '') {
    const host = document.getElementById('toastHost');
    if (!host) return;
    const t = SG.el('div', { class: 'toast ' + type, role: 'status', text: msg });
    host.append(t);
    setTimeout(() => t.remove(), 4200);
  };

  /* ---------- 画面の初期化 ---------- */
  document.addEventListener('DOMContentLoaded', () => {
    const sel = document.getElementById('langSelect');
    if (sel) sel.addEventListener('change', () => SG.setLang(sel.value));

    const toggle = document.getElementById('menuToggle');
    const nav = document.getElementById('mainNav');
    if (toggle && nav) {
      toggle.addEventListener('click', () => {
        const open = nav.classList.toggle('is-open');
        toggle.setAttribute('aria-expanded', String(open));
      });
    }

    const setOnline = (online, announce) => {
      document.body.classList.toggle('is-offline', !online);
      if (announce) SG.toast(SG.t(online ? 'common.online' : 'common.offline'), online ? 'ok' : '');
    };
    setOnline(navigator.onLine, false);
    window.addEventListener('online', () => setOnline(true, true));
    window.addEventListener('offline', () => setOnline(false, true));
  });

  /* ---------- Service Worker（HTTPS または localhost でのみ動く） ---------- */
  if ('serviceWorker' in navigator && (location.protocol === 'https:' || location.hostname === 'localhost')) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('/sw.js').catch(() => { /* 失敗してもアプリは動く */ });
    });
  }
})();
