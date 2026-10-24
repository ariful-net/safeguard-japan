/* =========================================================
   SafeGuard Japan — prepare.js
   備蓄量カリキュレーター・チェックリスト（端末内保存）・災害別タブ
   ========================================================= */
(function () {
  'use strict';
  const SG = window.SG;
  const CHECK_KEY = 'sg_prepare_checks';
  const CALC_KEY = 'sg_prepare_calc';

  /* 目安（内閣府・自治体の一般的な基準） */
  const PER_DAY = { water: 3, meals: 3, toilet: 5 };
  const GAS_PER_WEEK = 6;

  function calc() {
    const peopleEl = document.getElementById('calcPeople');
    const people = Math.min(10, Math.max(1, parseInt(peopleEl.value, 10) || 1));
    peopleEl.value = people;
    const days = parseInt((document.querySelector('input[name="days"]:checked') || {}).value || '3', 10);
    document.getElementById('outWater').textContent = people * days * PER_DAY.water;
    document.getElementById('outMeals').textContent = people * days * PER_DAY.meals;
    document.getElementById('outToilet').textContent = people * days * PER_DAY.toilet;
    document.getElementById('outGas').textContent = Math.ceil(people * GAS_PER_WEEK * days / 7);
    SG.store.set(CALC_KEY, { people, days });
  }

  function initCalc() {
    if (!document.getElementById('calc')) return;
    const saved = SG.store.get(CALC_KEY);
    if (saved) {
      document.getElementById('calcPeople').value = saved.people;
      const r = document.querySelector('input[name="days"][value="' + saved.days + '"]');
      if (r) r.checked = true;
    }
    document.querySelectorAll('[data-step]').forEach((b) => {
      b.addEventListener('click', () => {
        const input = document.getElementById('calcPeople');
        input.value = (parseInt(input.value, 10) || 1) + parseInt(b.dataset.step.split(':')[1], 10);
        calc();
      });
    });
    document.getElementById('calcPeople').addEventListener('input', calc);
    document.querySelectorAll('input[name="days"]').forEach((r) => r.addEventListener('change', calc));
    calc();
  }

  function initChecklist() {
    const boxes = Array.from(document.querySelectorAll('.checklist input[type="checkbox"][data-key]'));
    if (!boxes.length) return;
    const saved = SG.store.get(CHECK_KEY, {});
    boxes.forEach((b) => { b.checked = !!saved[b.dataset.key]; });

    const update = () => {
      const state = {};
      boxes.forEach((b) => { if (b.checked) state[b.dataset.key] = true; });
      SG.store.set(CHECK_KEY, state);
      const pct = Math.round(Object.keys(state).length / boxes.length * 100);
      document.getElementById('progNum').textContent = pct;
      document.getElementById('progBar').style.width = pct + '%';
    };
    boxes.forEach((b) => b.addEventListener('change', update));
    document.getElementById('checkReset').addEventListener('click', () => {
      boxes.forEach((b) => { b.checked = false; });
      update();
    });
    update();
  }

  function initTabs() {
    const root = document.getElementById('during');
    if (!root) return;
    const tabs = Array.from(root.querySelectorAll('[role="tab"]'));
    tabs.forEach((tab) => {
      tab.addEventListener('click', () => {
        tabs.forEach((t) => {
          const on = t === tab;
          t.classList.toggle('is-active', on);
          t.setAttribute('aria-selected', String(on));
          document.getElementById(t.getAttribute('aria-controls')).hidden = !on;
        });
      });
    });
  }

  document.addEventListener('DOMContentLoaded', () => {
    initCalc();
    initChecklist();
    initTabs();
  });
})();
