/* =========================================================
   SafeGuard Japan — story-video.js
   ストーリー動画: 選択言語の字幕を自動で表示、チャプター移動
   ========================================================= */
(function () {
  'use strict';
  const SG = window.SG;

  document.addEventListener('DOMContentLoaded', () => {
    const video = document.getElementById('storyVideo');
    const list = document.getElementById('storyChapters');
    if (!video) return;

    // 選択中の言語の字幕だけ表示する
    const showTrack = () => {
      Array.from(video.textTracks).forEach((tr) => {
        tr.mode = tr.language === SG.lang ? 'showing' : 'disabled';
      });
    };
    showTrack();
    video.addEventListener('loadedmetadata', showTrack);

    if (!list) return;
    const buttons = Array.from(list.querySelectorAll('button[data-t]'));
    const times = buttons.map((b) => parseFloat(b.dataset.t));

    buttons.forEach((b) => {
      b.addEventListener('click', () => {
        video.currentTime = parseFloat(b.dataset.t);
        video.play().catch(() => { /* 自動再生がブロックされても無視 */ });
      });
    });

    video.addEventListener('timeupdate', () => {
      let idx = 0;
      times.forEach((t, i) => { if (video.currentTime >= t) idx = i; });
      buttons.forEach((b, i) => b.classList.toggle('is-current', i === idx));
    });
  });
})();
