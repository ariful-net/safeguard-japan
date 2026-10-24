/* =========================================================
   SafeGuard Japan — safety.js
   安否メッセージ作成（詳細設計書 9.3）
   定型文 + 場所 + 現在地リンクを組み立て → Web Share API
   → 失敗時はクリップボードにコピー。サーバーには送信しない。
   ========================================================= */
(function () {
  'use strict';
  const SG = window.SG;
  const t = SG.t;
  const MAX = 200; // 追加メッセージの上限（詳細設計書 7章）

  const $ = (id) => document.getElementById(id);
  let mapLink = null;

  function selected() {
    return document.querySelector('input[name="status"]:checked');
  }

  function nowText() {
    return SG.formatTime(new Date().toISOString(), { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  function build() {
    const st = selected();
    const place = $('sfPlace').value.trim();
    const extra = $('sfExtra').value.trim();
    const addJa = $('sfJa') ? $('sfJa').checked : false;
    const lines = [];

    if (st) lines.push('【' + st.dataset.text + '】');
    if (st && addJa && SG.lang !== 'ja') lines.push('（' + st.dataset.textJa + '）');
    lines.push(t('safety.time') + ': ' + nowText());
    if (place) lines.push(t('safety.place') + ': ' + place);
    if (mapLink) lines.push(t('safety.map') + ': ' + mapLink);
    if (extra) lines.push('', extra);
    lines.push('', '— ' + t('safety.signature'));
    return lines.join('\n');
  }

  function validate() {
    const len = $('sfExtra').value.length;
    const over = len > MAX;
    $('sfCounter').textContent = len + '/' + MAX;
    $('sfCounter').classList.toggle('is-over', over);
    $('sfExtra').classList.toggle('is-invalid', over);
    $('sfExtra').setAttribute('aria-invalid', String(over));
    $('sfError').textContent = over ? t('safety.error.length') : '';
    $('sfShare').disabled = over;
    $('sfCopy').disabled = over;
    return !over;
  }

  function refresh() {
    validate();
    $('sfPreview').textContent = build();
  }

  async function copy(text) {
    try {
      await navigator.clipboard.writeText(text);
    } catch (e) {
      // 古いブラウザ用
      const ta = SG.el('textarea', { style: 'position:fixed;opacity:0' });
      ta.value = text; document.body.append(ta); ta.select();
      document.execCommand('copy'); ta.remove();
    }
    SG.toast(t('safety.copied'), 'ok');
  }

  function goSent() {
    const url = $('safetyForm').dataset.sentUrl;
    if (url) setTimeout(() => { location.href = url; }, 900);
  }

  async function share() {
    if (!validate()) return;
    const text = build();
    if (navigator.share) {
      try {
        await navigator.share({ text });
        goSent();
        return;
      } catch (e) {
        if (e && e.name === 'AbortError') return; // ユーザーが閉じた
      }
    }
    // Web Share 非対応・失敗時はコピーだけ（貼り付けはユーザーが行うので完了画面には進まない）
    await copy(text);
  }

  async function toggleLocation() {
    if (!$('sfLoc').checked) { mapLink = null; refresh(); return; }
    SG.toast(t('safety.locating'));
    const pos = await SG.getPosition({ timeout: 10000 });
    if (pos.isDefault) {
      $('sfLoc').checked = false;
      mapLink = null;
      SG.toast(t('safety.locationFailed'));
    } else {
      mapLink = 'https://maps.google.com/?q=' + pos.lat.toFixed(5) + ',' + pos.lon.toFixed(5);
    }
    refresh();
  }

  document.addEventListener('DOMContentLoaded', () => {
    if (!$('safetyForm')) return;
    // 避難所ナビで到着した場所を引き継ぐ
    try {
      const place = sessionStorage.getItem('sg_safety_place');
      if (place) $('sfPlace').value = place;
    } catch (e) { /* noop */ }

    document.querySelectorAll('input[name="status"]').forEach((r) => r.addEventListener('change', refresh));
    ['sfPlace', 'sfExtra'].forEach((id) => $(id).addEventListener('input', refresh));
    if ($('sfJa')) $('sfJa').addEventListener('change', refresh);
    $('sfLoc').addEventListener('change', toggleLocation);
    $('sfShare').addEventListener('click', share);
    $('sfCopy').addEventListener('click', () => { if (validate()) copy(build()); });
    $('safetyForm').addEventListener('submit', (e) => e.preventDefault());
    refresh();
  });
})();
