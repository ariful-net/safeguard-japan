/* =========================================================
   SafeGuard Japan — Service Worker
   ネット切断時は、保存した画面と静的ファイルを使う（詳細設計書 2.2）。
   - 画面(HTML): ネット優先 → 失敗時キャッシュ → それも無ければ /offline
   - CSS/JS/画像: キャッシュを返しつつ裏で更新（stale-while-revalidate）
   - /api/*: ネットのみ（避難所結果は JS が LocalStorage に保存する）
   - 地図タイル・動画: キャッシュしない（タイルはオンライン時のみ / 動画は容量が大きい）
   更新時は VERSION を上げること。
   ========================================================= */
const VERSION = 'sg-v1.0.0';
const PAGE_CACHE = VERSION + '-pages';
const ASSET_CACHE = VERSION + '-assets';
const FONT_CACHE = 'sg-fonts';

const PAGES = ['/', '/shelters', '/prepare', '/safety-status', '/alert-info', '/safety-status/sent', '/offline'];
const ASSETS = [
  '/css/base.css', '/css/home.css', '/css/shelters.css', '/css/pages.css',
  '/js/core.js', '/js/weather.js', '/js/alert.js', '/js/shelters.js', '/js/safety.js', '/js/prepare.js', '/js/story-video.js',
  '/vendor/leaflet/leaflet.js', '/vendor/leaflet/leaflet.css',
  '/vendor/leaflet/images/marker-icon.png', '/vendor/leaflet/images/marker-shadow.png',
  '/img/hero-hanshin.jpg', '/img/icon-192.png', '/img/favicon.png',
  '/video/evacuation-story-poster.jpg',
  '/manifest.json'
];

self.addEventListener('install', (event) => {
  event.waitUntil((async () => {
    const assets = await caches.open(ASSET_CACHE);
    await assets.addAll(ASSETS);
    const pages = await caches.open(PAGE_CACHE);
    // 1画面の失敗でインストール全体が止まらないよう個別に保存
    await Promise.all(PAGES.map((p) => pages.add(p).catch(() => null)));
    await self.skipWaiting();
  })());
});

self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    const keys = await caches.keys();
    await Promise.all(keys.filter((k) => k.startsWith('sg-v') && !k.startsWith(VERSION)).map((k) => caches.delete(k)));
    await self.clients.claim();
  })());
});

self.addEventListener('fetch', (event) => {
  const req = event.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);

  // Google Fonts: 一度取得したらキャッシュ優先
  if (url.hostname === 'fonts.googleapis.com' || url.hostname === 'fonts.gstatic.com') {
    event.respondWith(cacheFirst(req, FONT_CACHE));
    return;
  }
  if (url.origin !== self.location.origin) return;          // 地図タイル・ルート API などはそのまま
  if (url.pathname.startsWith('/api/')) return;              // API はネットのみ
  if (url.pathname.startsWith('/video/') && !url.pathname.endsWith('.jpg') && !url.pathname.endsWith('.vtt')) return;

  if (req.mode === 'navigate') {
    event.respondWith(networkFirstPage(req));
    return;
  }
  event.respondWith(staleWhileRevalidate(req));
});

async function networkFirstPage(req) {
  const cache = await caches.open(PAGE_CACHE);
  try {
    const res = await fetch(req);
    if (res.ok) cache.put(req, res.clone());
    return res;
  } catch (e) {
    return (await cache.match(req))
        || (await cache.match(req, { ignoreSearch: true }))
        || (await cache.match('/offline'))
        || new Response('Offline', { status: 503, headers: { 'Content-Type': 'text/plain; charset=utf-8' } });
  }
}

async function staleWhileRevalidate(req) {
  const cache = await caches.open(ASSET_CACHE);
  const cached = await cache.match(req);
  const network = fetch(req).then((res) => {
    if (res.ok) cache.put(req, res.clone());
    return res;
  }).catch(() => null);
  return cached || (await network) || new Response('', { status: 504 });
}

async function cacheFirst(req, name) {
  const cache = await caches.open(name);
  const cached = await cache.match(req);
  if (cached) return cached;
  try {
    const res = await fetch(req);
    if (res.ok || res.type === 'opaque') cache.put(req, res.clone());
    return res;
  } catch (e) {
    return new Response('', { status: 504 });
  }
}
