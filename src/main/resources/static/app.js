'use strict';

async function api(path, options = {}) {
  const opts = Object.assign({ headers: { 'Content-Type': 'application/json' } }, options);
  const res = await fetch(path, opts);
  let body = null;
  try { body = await res.json(); } catch (e) { /* ignore */ }
  if (res.status === 401) {
    location.href = '/login.html';
    throw new Error('需要访问口令');
  }
  if (!res.ok || !body || body.code !== 0) {
    const msg = (body && body.msg) || ('请求失败 HTTP ' + res.status);
    throw new Error(msg);
  }
  return body.data;
}

function esc(s) {
  if (s === null || s === undefined) return '';
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function toast(msg) {
  let el = document.getElementById('toast');
  if (!el) {
    el = document.createElement('div');
    el.id = 'toast';
    document.body.appendChild(el);
  }
  el.textContent = msg;
  el.classList.add('show');
  clearTimeout(el._timer);
  el._timer = setTimeout(() => el.classList.remove('show'), 2600);
}

function diffLabel(d) {
  if (d >= 3) return ['难', 'hard'];
  if (d === 2) return ['中', 'mid'];
  return ['易', 'easy'];
}

function parseJsonArr(s, fallback) {
  try {
    const v = JSON.parse(s);
    return Array.isArray(v) ? v : fallback;
  } catch (e) {
    return fallback;
  }
}

const page = document.body.dataset.page;

document.addEventListener('DOMContentLoaded', () => {
  if (page === 'question') initQuestion();
  else if (page === 'answer') initAnswer();
  else if (page === 'review') initReview();
});

function initQuestion() {
  const state = { keyword: '', category: '', page: 1, size: 10 };
  const listEl = document.getElementById('list');
  const emptyEl = document.getElementById('empty');
  const keywordEl = document.getElementById('keyword');
  const pageInfo = document.getElementById('pageInfo');

  async function load() {
    listEl.innerHTML = '<div class="skeleton"></div>'.repeat(6);
    const params = new URLSearchParams({ page: state.page, size: state.size });
    if (state.keyword) params.set('keyword', state.keyword);
    if (state.category) params.set('category', state.category);
    try {
      const data = await api('/api/questions?' + params.toString());
      render(data);
    } catch (e) {
      listEl.innerHTML = '';
      toast(e.message);
    }
  }

  function render(data) {
    const items = data.list || [];
    listEl.innerHTML = '';
    emptyEl.classList.toggle('hidden', items.length > 0);
    items.forEach(q => {
      const d = diffLabel(q.difficulty);
      const score = q.lastScore == null ? '—' : q.lastScore;
      const a = document.createElement('a');
      a.className = 'q-item';
      a.href = 'answer.html?id=' + q.id;
      a.innerHTML =
        '<div class="q-top">' +
          '<span class="badge">' + esc(q.category) + '</span>' +
          '<span class="diff ' + d[1] + '">' + d[0] + '</span>' +
        '</div>' +
        '<div class="q-title">' + esc(q.title) + '</div>' +
        '<div class="q-meta">上次得分：<b>' + esc(score) + '</b></div>';
      listEl.appendChild(a);
    });

    const totalPages = Math.max(1, Math.ceil((data.total || 0) / state.size));
    pageInfo.textContent = '第 ' + state.page + ' / ' + totalPages + ' 页 · 共 ' + data.total + ' 题';
    document.getElementById('prev').disabled = state.page <= 1;
    document.getElementById('next').disabled = state.page >= totalPages;
  }

  function search() {
    state.keyword = keywordEl.value.trim();
    state.page = 1;
    load();
  }

  document.getElementById('searchBtn').addEventListener('click', search);
  keywordEl.addEventListener('keydown', e => { if (e.key === 'Enter') search(); });
  document.querySelectorAll('#tabs .tab').forEach(tab => {
    tab.addEventListener('click', () => {
      document.querySelectorAll('#tabs .tab').forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      state.category = tab.dataset.category;
      state.page = 1;
      load();
    });
  });
  document.getElementById('prev').addEventListener('click', () => { if (state.page > 1) { state.page--; load(); } });
  document.getElementById('next').addEventListener('click', () => { state.page++; load(); });

  load();
}

function initAnswer() {
  const qid = new URLSearchParams(location.search).get('id');
  if (!qid) { location.href = 'question.html'; return; }

  const card = document.getElementById('questionCard');
  const answerEl = document.getElementById('answer');
  const countEl = document.getElementById('count');
  const resultEl = document.getElementById('result');

  answerEl.addEventListener('input', () => { countEl.textContent = answerEl.value.length + ' 字'; });

  async function loadQuestion() {
    try {
      const q = await api('/api/questions/' + qid);
      const d = diffLabel(q.difficulty);
      const points = parseJsonArr(q.keyPoints, []).map(p => '<span class="chip">' + esc(p) + '</span>').join('');
      card.innerHTML =
        '<div class="q-top">' +
          '<span class="badge">' + esc(q.category) + '</span>' +
          '<span class="diff ' + d[1] + '">' + d[0] + '</span>' +
          '<span class="src">' + esc(q.source || '') + '</span>' +
        '</div>' +
        '<h2 class="q-title">' + esc(q.title) + '</h2>' +
        (points ? '<div class="chips">' + points + '</div>' : '') +
        '<details class="answer-std"><summary>看参考答案（先答再看）</summary><p>' + esc(q.answerStandard || '暂无参考答案') + '</p></details>';

      if (q.lastAttemptId) {
        const a = await api('/api/attempts/' + q.lastAttemptId);
        answerEl.value = a.userAnswer || '';
        countEl.textContent = answerEl.value.length + ' 字';
        if (a.status === 'scored') renderResult(a);
        else if (a.status === 'failed') renderFailed(a);
      }
    } catch (e) {
      toast(e.message);
    }
  }

  function renderResult(a) {
    const points = parseJsonArr(a.missedPoints, []);
    resultEl.innerHTML =
      '<div class="result">' +
        '<div class="score"><span class="num">' + esc(a.score) + '</span><span class="unit">分</span></div>' +
        (points.length ? '<div class="block"><h4>漏掉的点</h4><ul>' + points.map(p => '<li>' + esc(p) + '</li>').join('') + '</ul></div>' : '') +
        (a.followup ? '<div class="block"><h4>追问</h4><blockquote>' + esc(a.followup) + '</blockquote></div>' : '') +
        (a.aiComment ? '<div class="block"><h4>点评</h4><p>' + esc(a.aiComment) + '</p></div>' : '') +
      '</div>';
  }

  function renderFailed(a) {
    resultEl.innerHTML =
      '<div class="result failed">' +
        '<p>AI 暂不可用，本次回答已保存，可稍后重试评分。</p>' +
        (a.aiComment ? '<p class="dim">' + esc(a.aiComment) + '</p>' : '') +
        '<button id="retry" class="ghost">重试评分</button>' +
      '</div>';
    const retry = document.getElementById('retry');
    if (retry) retry.addEventListener('click', evaluate);
  }

  async function evaluate() {
    const answer = answerEl.value.trim();
    if (answer.length < 10) { toast('回答太短，再写点吧（建议 80 字以上）'); return; }

    resultEl.innerHTML = '<div class="result loading"><div class="spinner"></div><p>评分中…最多等 20 秒</p></div>';

    let attempt;
    try {
      attempt = await api('/api/attempts', { method: 'POST', body: JSON.stringify({ questionId: Number(qid), userAnswer: answer }) });
    } catch (e) {
      toast(e.message);
      resultEl.innerHTML = '';
      return;
    }

    try {
      const scored = await api('/api/attempts/' + attempt.attemptId + '/evaluate', { method: 'POST' });
      if (scored.status === 'scored') renderResult(scored);
      else renderFailed(scored);
    } catch (e) {
      renderFailed({ aiComment: e.message });
    }
  }

  document.getElementById('submit').addEventListener('click', evaluate);
  document.getElementById('skip').addEventListener('click', () => { location.href = 'question.html'; });

  loadQuestion();
}

function initReview() {
  const listEl = document.getElementById('list');
  const countsEl = document.getElementById('counts');
  let state = { mastered: 0 };

  async function load() {
    try {
      const data = await api('/api/review?mastered=' + state.mastered);
      render(data);
    } catch (e) {
      toast(e.message);
    }
  }

  function render(data) {
    countsEl.textContent = '待复习 ' + data.pending + ' 题 / 已掌握 ' + data.mastered + ' 题';
    const items = data.list || [];
    listEl.innerHTML = '';
    if (!items.length) {
      listEl.innerHTML = '<div class="empty">' + (state.mastered === 0 ? '太棒了，没有待复习的题' : '还没有标记已掌握的题') + '</div>';
      return;
    }
    items.forEach(item => {
      const answer = (item.userAnswer || '').length > 100 ? item.userAnswer.slice(0, 100) + '…' : (item.userAnswer || '');
      const card = document.createElement('div');
      card.className = 'review-card';
      card.innerHTML =
        '<div class="q-top">' +
          '<span class="badge">' + esc(item.category) + '</span>' +
          '<span class="score-badge">' + (item.score == null ? '—' : item.score) + '</span>' +
        '</div>' +
        '<div class="q-title"><a href="answer.html?id=' + item.questionId + '">' + esc(item.title) + '</a></div>' +
        '<p class="dim answer-preview">' + esc(answer) + '</p>' +
        (item.aiComment ? '<p class="dim comment">点评：' + esc(item.aiComment) + '</p>' : '') +
        '<div class="review-actions">' +
          '<input class="note-input" placeholder="写点备注…" value="' + esc(item.note || '') + '">' +
          '<button class="save-note ghost">保存备注</button>' +
          '<button class="mastered">' + (state.mastered === 0 ? '已掌握' : '恢复待复习') + '</button>' +
        '</div>';

      const noteInput = card.querySelector('.note-input');
      card.querySelector('.save-note').addEventListener('click', async () => {
        try {
          await api('/api/review/' + item.id, { method: 'PATCH', body: JSON.stringify({ note: noteInput.value }) });
          toast('备注已保存');
        } catch (e) { toast(e.message); }
      });
      card.querySelector('.mastered').addEventListener('click', async () => {
        const target = state.mastered === 0 ? 1 : 0;
        try {
          await api('/api/review/' + item.id, { method: 'PATCH', body: JSON.stringify({ mastered: target }) });
          load();
        } catch (e) { toast(e.message); }
      });
      listEl.appendChild(card);
    });
  }

  document.querySelectorAll('#tabs .tab').forEach(tab => {
    tab.addEventListener('click', () => {
      document.querySelectorAll('#tabs .tab').forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      state.mastered = Number(tab.dataset.mastered);
      load();
    });
  });

  load();
}