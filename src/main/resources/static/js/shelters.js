/* =========================================================
   SafeGuard Japan — shelters.js
   避難所検索（/api/shelters）・地図表示（Leaflet + OSM）・
   徒歩ナビゲーション（ルート + 現在地追跡 + 到着判定）
   処理順は詳細設計書 9.1 に準拠:
   GPS取得 → API → ピンと一覧を描画 → LocalStorage にキャッシュ（オフライン用）
   ========================================================= */
(function () {
  'use strict';
  const SG = window.SG;
  const t = SG.t;

  const CACHE_KEY = 'sg_shelters_last';
  // 徒歩ルート: FOSSGIS の公開 OSRM サーバー（学習・小規模利用向け。本番は自前サーバー推奨）
  const ROUTE_URL = 'https://routing.openstreetmap.de/routed-foot/route/v1/driving/';
  const WALK_M_PER_MIN = 80;        // 詳細設計書: 分速80m
  const ARRIVE_M = 40;              // 到着判定
  const OFF_ROUTE_M = 60;           // ルート外れ判定
  const REROUTE_INTERVAL_MS = 20000;

  const $ = (id) => document.getElementById(id);
  const state = {
    map: null, youMarker: null, markers: new Map(), routeLine: null,
    pos: null, results: [], activeId: null,
    nav: null, watchId: null, wakeLock: null, lastReroute: 0
  };

  /* ================= 地図 ================= */
  function initMap() {
    if (typeof L === 'undefined') return;
    const c = SG.defaultLocation;
    state.map = L.map('map', { zoomControl: true, attributionControl: true }).setView([c.lat, c.lon], 14);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(state.map);
  }

  function setYou(pos) {
    if (!state.map) return;
    const icon = L.divIcon({ className: '', html: '<div class="you-dot' + (pos.isDefault ? ' is-default' : '') + '"></div>', iconSize: [18, 18], iconAnchor: [9, 9] });
    if (!state.youMarker) {
      state.youMarker = L.marker([pos.lat, pos.lon], { icon, zIndexOffset: 1000, keyboard: false }).addTo(state.map);
      state.youMarker.bindTooltip(t('sh.you'));
    } else {
      state.youMarker.setLatLng([pos.lat, pos.lon]).setIcon(icon);
    }
  }

  function pinIcon(n, active) {
    return L.divIcon({
      className: '',
      html: '<div class="pin' + (n === 1 ? ' is-first' : '') + (active ? ' is-active' : '') + '"><span>' + n + '</span></div>',
      iconSize: [30, 30], iconAnchor: [15, 30], popupAnchor: [0, -28]
    });
  }

  function popupNode(s) {
    return SG.el('div', {},
      SG.el('b', { text: s.name }), SG.el('br'),
      SG.el('span', { text: SG.formatDistance(s.distanceM) + ' · ' + t('sh.walk', s.walkMinutes) }), SG.el('br'),
      SG.el('a', { href: '#', onclick: (e) => { e.preventDefault(); startNav(s); }, text: t('sh.navigate') + ' →' })
    );
  }

  function drawMarkers() {
    if (!state.map) return;
    state.markers.forEach((m) => m.remove());
    state.markers.clear();
    const bounds = [];
    state.results.forEach((s, i) => {
      const m = L.marker([s.lat, s.lon], { icon: pinIcon(i + 1, false), title: s.name }).addTo(state.map);
      m.bindPopup(() => popupNode(s));
      m.on('click', () => selectItem(s.id, false));
      state.markers.set(s.id, m);
      bounds.push([s.lat, s.lon]);
    });
    if (state.pos) bounds.push([state.pos.lat, state.pos.lon]);
    if (bounds.length > 1) state.map.fitBounds(bounds, { padding: [40, 40], maxZoom: 16 });
    else if (state.pos) state.map.setView([state.pos.lat, state.pos.lon], 15);
  }

  /* ================= 一覧 ================= */
  function typeChips(s) {
    const chips = [];
    if (s.earthquake) chips.push(SG.el('span', { class: 'chip', text: t('type.earthquake') }));
    if (s.tsunami) chips.push(SG.el('span', { class: 'chip', text: t('type.tsunami') }));
    if (s.flood) chips.push(SG.el('span', { class: 'chip', text: t('type.flood') }));
    return chips;
  }

  function gmapsUrl(s) {
    return 'https://www.google.com/maps/dir/?api=1&travelmode=walking&destination=' + s.lat + ',' + s.lon;
  }

  function renderList(fromCache) {
    const list = $('shList');
    list.replaceChildren();
    $('shResults').hidden = false;
    $('shCount').textContent = String(state.results.length);

    if (!state.results.length) {
      setStatus(t('sh.none'), 'warn');
      return;
    }
    state.results.forEach((s, i) => {
      const li = SG.el('li', { class: 'sh-item', 'data-id': s.id, tabindex: '0' },
        SG.el('span', { class: 'sh-idx', text: String(i + 1) }),
        SG.el('div', {},
          SG.el('div', { class: 'sh-name' }, s.name, s.nameJa && s.nameJa !== s.name ? SG.el('span', { class: 'sh-name-ja', text: s.nameJa }) : null),
          SG.el('div', { class: 'sh-addr', text: s.address }),
          SG.el('div', { class: 'sh-meta' },
            SG.el('span', { class: 'sh-dist', text: SG.formatDistance(s.distanceM) }),
            SG.el('span', { class: 'sh-walk', text: t('sh.walk', s.walkMinutes) }),
            SG.el('span', { class: 'muted', text: s.capacity ? t('sh.capacity', s.capacity.toLocaleString()) : t('sh.capacityUnknown') })
          ),
          SG.el('div', { class: 'sh-tags' }, typeChips(s)),
          SG.el('div', { class: 'sh-item-actions' },
            SG.el('button', { type: 'button', class: 'btn btn-sm btn-primary', onclick: (e) => { e.stopPropagation(); startNav(s); }, text: t('sh.navigate') }),
            SG.el('a', { class: 'btn btn-sm', href: gmapsUrl(s), target: '_blank', rel: 'noopener', onclick: (e) => e.stopPropagation(), text: t('sh.gmaps') })
          )
        )
      );
      li.addEventListener('click', () => selectItem(s.id, true));
      li.addEventListener('keydown', (e) => { if (e.key === 'Enter') selectItem(s.id, true); });
      list.append(li);
    });
    if (fromCache) setStatus(t('sh.offlineCached'), 'warn');
  }

  function selectItem(id, fly) {
    state.activeId = id;
    document.querySelectorAll('.sh-item').forEach((el) => el.classList.toggle('is-active', String(el.dataset.id) === String(id)));
    const idx = state.results.findIndex((s) => s.id === id);
    state.markers.forEach((m, mid) => m.setIcon(pinIcon(state.results.findIndex((s) => s.id === mid) + 1, mid === id)));
    const m = state.markers.get(id);
    if (m && fly) { state.map.flyTo(m.getLatLng(), Math.max(state.map.getZoom(), 15), { duration: .6 }); m.openPopup(); }
    const item = document.querySelector('.sh-item[data-id="' + id + '"]');
    if (item && !fly) item.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    return idx;
  }

  function setStatus(msg, kind) {
    const el = $('shStatus');
    el.textContent = msg || '';
    el.className = 'sh-status' + (kind ? ' is-' + kind : '');
  }

  /* ================= 検索 ================= */
  function formParams() {
    const f = $('shForm');
    return {
      radius: parseInt(f.radius.value, 10) || 3,
      type: (f.querySelector('input[name="type"]:checked') || {}).value || ''
    };
  }

  async function search({ refreshPosition = true } = {}) {
    const btn = $('shGps');
    btn.disabled = true;
    try {
      if (refreshPosition || !state.pos) {
        setStatus(t('sh.gpsLocating'));
        state.pos = await SG.getPosition();
      }
      setYou(state.pos);
      const p = formParams();
      if (state.pos.isDefault) setStatus(t('sh.gpsDenied'), 'warn'); else setStatus('');

      const url = SG.withLang('/api/shelters', { lat: state.pos.lat.toFixed(6), lon: state.pos.lon.toFixed(6), radius: p.radius, type: p.type });
      const data = await SG.fetchJSON(url, { timeout: 8000 });
      state.results = data.shelters || [];
      SG.store.set(CACHE_KEY, { at: Date.now(), params: p, shelters: state.results });
      renderList(false);
      drawMarkers();
      if (state.results.length && !state.pos.isDefault) setStatus('');
    } catch (e) {
      if (e.status === 400 && e.body && e.body.errors) {
        setStatus(e.body.errors.join(' '), 'error');
      } else {
        loadCached() || setStatus(t('sh.error'), 'error');
      }
    } finally {
      btn.disabled = false;
    }
  }

  /** オフライン時: 前回の結果を、今の位置からの距離で並べ直して表示 */
  function loadCached() {
    const c = SG.store.get(CACHE_KEY);
    if (!c || !c.shelters || !c.shelters.length) return false;
    state.results = c.shelters.map((s) => {
      if (!state.pos) return s;
      const d = SG.distance(state.pos, s);
      return { ...s, distanceM: Math.round(d), walkMinutes: Math.ceil(d / WALK_M_PER_MIN) };
    }).sort((a, b) => a.distanceM - b.distanceM);
    renderList(true);
    drawMarkers();
    return true;
  }

  /* ================= ナビゲーション ================= */
  function maneuverText(step) {
    const m = step.maneuver || {};
    if (m.type === 'depart') return t('nav.depart');
    if (m.type === 'arrive') return t('nav.arrive');
    const map = {
      left: 'nav.turn.left', right: 'nav.turn.right',
      'slight left': 'nav.turn.slightLeft', 'slight right': 'nav.turn.slightRight',
      'sharp left': 'nav.turn.sharpLeft', 'sharp right': 'nav.turn.sharpRight',
      straight: 'nav.turn.straight', uturn: 'nav.turn.uturn'
    };
    const verb = t(map[m.modifier] || 'nav.turn.straight');
    return step.name ? verb + ' — ' + step.name : verb + ' — ' + t('nav.unnamedRoad');
  }

  async function fetchRoute(from, to) {
    const url = ROUTE_URL + from.lon + ',' + from.lat + ';' + to.lon + ',' + to.lat + '?overview=full&geometries=geojson&steps=true';
    const ctrl = new AbortController();
    const timer = setTimeout(() => ctrl.abort(), 8000);
    try {
      const res = await fetch(url, { signal: ctrl.signal });
      if (!res.ok) throw new Error('route ' + res.status);
      const json = await res.json();
      const r = json.routes && json.routes[0];
      if (!r) throw new Error('no route');
      return {
        coords: r.geometry.coordinates.map(([lon, lat]) => ({ lat, lon })),
        steps: (r.legs[0] && r.legs[0].steps) || [],
        distance: r.distance
      };
    } finally {
      clearTimeout(timer);
    }
  }

  function drawRoute(coords, dashed) {
    if (!state.map) return;
    if (state.routeLine) state.routeLine.remove();
    state.routeLine = L.polyline(coords.map((c) => [c.lat, c.lon]), {
      color: '#e8334a', weight: 5, opacity: .95, dashArray: dashed ? '8 10' : null, lineCap: 'round'
    }).addTo(state.map);
    state.map.fitBounds(state.routeLine.getBounds(), { padding: [50, 50], maxZoom: 17 });
  }

  function renderSteps(steps, nextIdx) {
    const ol = $('navSteps');
    ol.replaceChildren();
    steps.forEach((s, i) => {
      ol.append(SG.el('li', { class: i === nextIdx ? 'is-next' : '' },
        SG.el('span', { text: maneuverText(s) }),
        SG.el('span', { class: 'd', text: s.distance ? SG.formatDistance(s.distance) : '' })));
    });
  }

  /** ルート上の残り距離（最寄り頂点から終点まで + 現在地から最寄り頂点まで） */
  function remainingOnRoute(pos, coords) {
    let best = Infinity, bi = 0;
    coords.forEach((c, i) => { const d = SG.distance(pos, c); if (d < best) { best = d; bi = i; } });
    let rest = 0;
    for (let i = bi; i < coords.length - 1; i++) rest += SG.distance(coords[i], coords[i + 1]);
    return { remaining: rest + best, offRoute: best, nearestIdx: bi };
  }

  function updateNavUI(pos) {
    const nav = state.nav;
    if (!nav) return;
    const dest = nav.dest;
    const straight = SG.distance(pos, dest);
    const brg = SG.bearing(pos, dest);
    $('navArrow').style.transform = 'rotate(' + brg + 'deg)';
    $('navDir').textContent = SG.compass(brg);

    let remaining = straight;
    if (nav.route) {
      const r = remainingOnRoute(pos, nav.route.coords);
      remaining = r.remaining;
      // ルートから外れたら再計算（間隔を空ける）
      if (r.offRoute > OFF_ROUTE_M && Date.now() - state.lastReroute > REROUTE_INTERVAL_MS) {
        state.lastReroute = Date.now();
        buildRoute(pos, dest, true);
      }
      // 次の曲がり角を強調
      let acc = 0, nextIdx = 0;
      const total = nav.route.distance;
      const done = Math.max(0, total - remaining);
      nav.route.steps.forEach((s, i) => { if (acc <= done) nextIdx = i; acc += s.distance || 0; });
      renderSteps(nav.route.steps, Math.min(nextIdx + 1, nav.route.steps.length - 1));
    }
    $('navRemain').textContent = SG.formatDistance(remaining);
    $('navEta').textContent = t('nav.eta', Math.max(1, Math.ceil(remaining / WALK_M_PER_MIN)));

    if (straight <= ARRIVE_M) arrived();
  }

  async function buildRoute(from, dest, silent) {
    const msg = $('navMsg');
    if (!silent) { msg.className = 'sh-nav-msg'; msg.textContent = t('nav.routing'); }
    try {
      const route = await fetchRoute(from, dest);
      state.nav.route = route;
      drawRoute(route.coords, false);
      renderSteps(route.steps, 1);
      msg.textContent = state.watchId != null ? t('nav.tracking') : '';
    } catch (e) {
      // オフライン・ルートサーバー停止時: 直線と方角で案内
      state.nav.route = null;
      drawRoute([from, dest], true);
      $('navSteps').replaceChildren();
      msg.className = 'sh-nav-msg';
      msg.textContent = t('nav.fallback') + ' (' + t('nav.straight') + ': ' + SG.formatDistance(SG.distance(from, dest)) + ')';
    }
    updateNavUI(from);
  }

  async function startNav(s) {
    stopNav(false);
    selectItem(s.id, false);
    state.nav = { dest: { lat: s.lat, lon: s.lon }, shelter: s, route: null };
    $('shNav').hidden = false;
    $('shResults').hidden = true;
    $('navDest').textContent = s.name;
    $('navGmaps').href = gmapsUrl(s);
    $('navSafety').hidden = true;
    try { sessionStorage.setItem('sg_safety_place', s.name); } catch (e) { /* noop */ }
    $('shNav').scrollIntoView({ block: 'start', behavior: 'smooth' });

    const from = state.pos || await SG.getPosition();
    await buildRoute(from, state.nav.dest, false);

    // 現在地の追跡（デフォルト位置のときは追跡しない）
    if (!from.isDefault && 'geolocation' in navigator) {
      state.watchId = navigator.geolocation.watchPosition(
        (p) => {
          state.pos = { lat: p.coords.latitude, lon: p.coords.longitude, isDefault: false };
          setYou(state.pos);
          updateNavUI(state.pos);
        },
        () => { /* 追跡失敗時は最後の位置のまま */ },
        { enableHighAccuracy: true, maximumAge: 5000, timeout: 15000 }
      );
      if (state.nav && state.nav.route) $('navMsg').textContent = t('nav.tracking');
    }
    // 案内中は画面を消さない（対応端末のみ）
    try { if ('wakeLock' in navigator) state.wakeLock = await navigator.wakeLock.request('screen'); } catch (e) { /* noop */ }
  }

  function arrived() {
    if (!state.nav || state.nav.arrived) return;
    state.nav.arrived = true;
    if (state.watchId != null) { navigator.geolocation.clearWatch(state.watchId); state.watchId = null; }
    const msg = $('navMsg');
    msg.className = 'sh-nav-msg is-ok';
    msg.textContent = t('nav.arrived');
    $('navSafety').hidden = false;
    if (navigator.vibrate) navigator.vibrate([120, 60, 120]);
  }

  function stopNav(showList = true) {
    if (state.watchId != null) { navigator.geolocation.clearWatch(state.watchId); state.watchId = null; }
    if (state.wakeLock) { state.wakeLock.release().catch(() => {}); state.wakeLock = null; }
    if (state.routeLine) { state.routeLine.remove(); state.routeLine = null; }
    state.nav = null;
    $('shNav').hidden = true;
    if (showList) {
      $('shResults').hidden = false;
      drawMarkers();
    }
  }

  /* ================= 初期化 ================= */
  document.addEventListener('DOMContentLoaded', () => {
    initMap();
    const form = $('shForm');
    const out = $('shRadiusOut');
    form.radius.addEventListener('input', () => { out.textContent = form.radius.value + ' km'; });
    form.radius.addEventListener('change', () => search({ refreshPosition: false }));
    form.querySelectorAll('input[name="type"]').forEach((r) => r.addEventListener('change', () => search({ refreshPosition: false })));
    form.addEventListener('submit', (e) => { e.preventDefault(); search({ refreshPosition: true }); });
    $('navStop').addEventListener('click', () => stopNav(true));

    // 警報詳細から来たときは災害種別を引き継ぐ（/shelters?type=tsunami）
    const qType = new URLSearchParams(location.search).get('type');
    const preset = qType && form.querySelector('input[name="type"][value="' + CSS.escape(qType) + '"]');
    if (preset) preset.checked = true;

    // 画面を開いたらすぐ検索（災害時は操作を1つでも減らす）
    search({ refreshPosition: true });
  });
})();
