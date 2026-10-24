/* =========================================================
   SafeGuard Japan — alert.js
   ホームの「最新の地震情報」カードを /api/alert で60秒ごとに更新
   ========================================================= */
(function () {
  'use strict';
  const SG = window.SG;
  const INTERVAL_MS = 60 * 1000;
  const CACHE_KEY = 'sg_alert';

  function render(card, a) {
    const body = document.getElementById('alertBody');
    if (!body) return;
    body.replaceChildren();
    card.dataset.status = a.status;
    card.dataset.level = a.level || 'none';

    if (a.status === 'OK') {
      const meta = SG.el('div', { class: 'alert-meta' },
        SG.el('span', {}, SG.el('span', { class: 'muted', text: SG.t('alert.area') }), ' ', SG.el('b', { text: a.area || '—' })),
        a.magnitude ? SG.el('span', {}, SG.el('span', { class: 'muted', text: SG.t('alert.magnitude') }), ' ', SG.el('b', { text: a.magnitude })) : null
      );
      body.append(
        SG.el('div', { class: 'alert-shindo', text: a.intensityText }),
        meta,
        SG.el('time', { class: 'alert-time mono', datetime: a.reportedAt, text: SG.formatTime(a.reportedAt, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) }),
        SG.el('p', { class: 'alert-msg', text: SG.t('alert.level.' + a.level) }),
        a.stale ? SG.el('p', { class: 'alert-stale', text: SG.t('alert.stale') }) : null
      );
    } else {
      body.append(SG.el('p', { class: 'alert-msg', text: SG.t(a.status === 'NONE' ? 'alert.none' : 'alert.offline') }));
    }
  }

  async function refresh(card) {
    try {
      const a = await SG.fetchJSON(SG.withLang('/api/alert'), { timeout: 6000 });
      if (a && a.status === 'OK') SG.store.set(CACHE_KEY + '_' + SG.lang, a);
      render(card, a);
    } catch (e) {
      const cached = SG.store.get(CACHE_KEY + '_' + SG.lang);
      render(card, cached ? { ...cached, stale: true } : { status: 'OFFLINE', level: 'none' });
    }
  }

  document.addEventListener('DOMContentLoaded', () => {
    const card = document.getElementById('alertCard');
    if (!card) return;
    // サーバー描画済みの時刻を読みやすい形式に
    const t = card.querySelector('time[datetime]');
    if (t && t.getAttribute('datetime')) t.textContent = SG.formatTime(t.getAttribute('datetime'), { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    setInterval(() => { if (!document.hidden) refresh(card); }, INTERVAL_MS);
    document.addEventListener('visibilitychange', () => { if (!document.hidden) refresh(card); });
  });
})();
