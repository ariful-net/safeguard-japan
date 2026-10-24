/* =========================================================
   SafeGuard Japan — weather.js
   画面最上部の天気バー：現在の天気 + これから12時間 + 注意喚起
   ========================================================= */
(function () {
  'use strict';
  const SG = window.SG;
  const REFRESH_MS = 10 * 60 * 1000;
  const CACHE_KEY = 'sg_weather';

  /* WMO 天気コード → 種類 */
  function kind(code) {
    if (code == null) return 'cloudy';
    if (code === 0) return 'clear';
    if (code === 1) return 'mainlyClear';
    if (code === 2) return 'partly';
    if (code === 3) return 'cloudy';
    if (code === 45 || code === 48) return 'fog';
    if (code >= 51 && code <= 57) return 'drizzle';
    if (code >= 61 && code <= 67) return 'rain';
    if ((code >= 71 && code <= 77) || code === 85 || code === 86) return 'snow';
    if (code >= 80 && code <= 82) return 'showers';
    if (code >= 95) return 'thunder';
    return 'cloudy';
  }

  /* 小さな線画アイコン（外部画像なし・オフラインでも表示） */
  const SUN = '<circle cx="12" cy="12" r="4.2" fill="#e2b66d"/><g stroke="#e2b66d" stroke-width="1.6" stroke-linecap="round"><path d="M12 2.5v2.2M12 19.3v2.2M2.5 12h2.2M19.3 12h2.2M5.3 5.3l1.6 1.6M17.1 17.1l1.6 1.6M5.3 18.7l1.6-1.6M17.1 6.9l1.6-1.6"/></g>';
  const CLOUD = '<path d="M7 18h10.2a4 4 0 0 0 .4-8 5.5 5.5 0 0 0-10.6 1.4A3.3 3.3 0 0 0 7 18z" fill="#cfcac0"/>';
  const ICONS = {
    clear: SUN,
    mainlyClear: SUN,
    partly: '<g transform="translate(-3 -3) scale(.8)">' + SUN + '</g><path d="M9 19h9a3.4 3.4 0 0 0 .3-6.8 4.7 4.7 0 0 0-9 1.2A2.8 2.8 0 0 0 9 19z" fill="#cfcac0"/>',
    cloudy: CLOUD,
    fog: '<g stroke="#cfcac0" stroke-width="1.8" stroke-linecap="round"><path d="M4 9h16M6 13h12M4 17h16"/></g>',
    drizzle: CLOUD.replace('18h', '14h').replace('18z', '14z') + '<g stroke="#8fc3ff" stroke-width="1.6" stroke-linecap="round"><path d="M9 17v1.5M13 17v1.5M17 17v1.5"/></g>',
    rain: '<path d="M7 14h10.2a4 4 0 0 0 .4-8 5.5 5.5 0 0 0-10.6 1.4A3.3 3.3 0 0 0 7 14z" fill="#cfcac0"/><g stroke="#8fc3ff" stroke-width="1.8" stroke-linecap="round"><path d="M8 16.5l-1 3M12 16.5l-1 3M16 16.5l-1 3"/></g>',
    showers: '<path d="M7 14h10.2a4 4 0 0 0 .4-8 5.5 5.5 0 0 0-10.6 1.4A3.3 3.3 0 0 0 7 14z" fill="#cfcac0"/><g stroke="#8fc3ff" stroke-width="1.8" stroke-linecap="round"><path d="M10 16.5l-1 3M14 16.5l-1 3"/></g>',
    snow: '<path d="M7 14h10.2a4 4 0 0 0 .4-8 5.5 5.5 0 0 0-10.6 1.4A3.3 3.3 0 0 0 7 14z" fill="#cfcac0"/><g fill="#fff"><circle cx="9" cy="18" r="1.1"/><circle cx="13" cy="19.5" r="1.1"/><circle cx="16.5" cy="17.5" r="1.1"/></g>',
    thunder: '<path d="M7 13h10.2a4 4 0 0 0 .4-8 5.5 5.5 0 0 0-10.6 1.4A3.3 3.3 0 0 0 7 13z" fill="#a9a49b"/><path d="M12.5 13.5l-3 5h3l-1.5 4 4.5-6h-3l1.5-3z" fill="#e2b66d"/>'
  };
  function icon(k) {
    const s = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    s.setAttribute('viewBox', '0 0 24 24');
    s.setAttribute('class', 'wx-icon');
    s.setAttribute('aria-hidden', 'true');
    s.innerHTML = ICONS[k] || ICONS.cloudy; // 固定の SVG 文字列のみ（外部データは入れない）
    return s;
  }

  const hourLabel = (iso) => {
    const h = parseInt(String(iso).slice(11, 13), 10);
    if (SG.lang === 'ja') return h + '時';
    return String(h).padStart(2, '0') + ':00';
  };

  function render(data, pos) {
    const now = document.getElementById('wxNow');
    const hours = document.getElementById('wxHours');
    const warn = document.getElementById('wxWarn');
    if (!now || !hours) return;
    now.replaceChildren(); hours.replaceChildren(); warn.replaceChildren();

    if (!data || data.status !== 'OK' || !data.current) {
      now.append(SG.el('span', { class: 'wx-state', text: SG.t('weather.offline') }));
      return;
    }
    const c = data.current, k = kind(c.code);
    now.append(
      SG.el('span', { class: 'wx-place' + (pos.isDefault ? ' is-default' : ''), title: SG.t('weather.source') },
        SG.el('span', { class: 'wx-place-text', text: pos.isDefault ? SG.t('weather.default') : SG.t('weather.here') })),
      icon(k),
      SG.el('span', { class: 'wx-temp', text: Math.round(c.tempC) + '°' }),
      SG.el('span', { class: 'wx-desc muted', text: SG.t('wx.' + k) })
    );

    (data.hourly || []).forEach((h) => {
      const pp = h.precipProb == null ? null : h.precipProb;
      hours.append(SG.el('div', { class: 'wx-hour', title: SG.t('wx.' + kind(h.code)) },
        SG.el('time', { datetime: h.time, text: hourLabel(h.time) }),
        icon(kind(h.code)),
        SG.el('span', { text: Math.round(h.tempC) + '°' }),
        pp != null && pp >= 20 ? SG.el('span', { class: 'pp' + (pp >= 60 ? ' hi' : ''), text: pp + '%' }) : null
      ));
    });

    (data.warnings || []).forEach((w) => {
      const red = w === 'thunder' || w === 'heavyRain';
      warn.append(SG.el('span', { class: 'chip ' + (red ? 'chip-red' : 'chip-amber'), text: SG.t('weather.warn.' + w) }));
    });
  }

  /* ページを開いただけで位置情報の許可ダイアログを出さない。
     すでに許可済みのときだけ GPS を使い、それ以外はデフォルト位置で表示する。 */
  async function resolvePosition() {
    const last = SG.lastPosition();
    if (last) return last;
    try {
      if (navigator.permissions) {
        const st = await navigator.permissions.query({ name: 'geolocation' });
        if (st.state === 'granted') return SG.getPosition({ timeout: 6000, maxAge: 10 * 60 * 1000 });
      }
    } catch (e) { /* Safari の一部は未対応 */ }
    return { ...SG.defaultLocation, isDefault: true };
  }

  async function load() {
    const pos = await resolvePosition();
    try {
      const data = await SG.fetchJSON(SG.withLang('/api/weather', { lat: pos.lat.toFixed(2), lon: pos.lon.toFixed(2) }));
      if (data && data.status === 'OK') SG.store.set(CACHE_KEY, { data, isDefault: pos.isDefault });
      render(data, pos);
    } catch (e) {
      const cached = SG.store.get(CACHE_KEY);
      render(cached ? cached.data : null, { isDefault: cached ? cached.isDefault : true });
    }
  }

  document.addEventListener('DOMContentLoaded', () => {
    if (!document.getElementById('wxBar')) return;
    load();
    setInterval(load, REFRESH_MS);
  });
})();
