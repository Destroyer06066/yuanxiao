/**
 * 报名系统演示页面 - JavaScript
 * 模拟考生端与院校管理平台的完整交互
 */

const API_BASE = ''; // 与 mock-server 同源

let currentUser = null;
let currentPushRecords = [];
let currentNotifications = [];
let pollingTimer = null;
let modalContext = {}; // 保存模态框上下文

// ==================== 初始化 ====================

document.addEventListener('DOMContentLoaded', () => {
  loadDemoAccounts();
  // 检查是否已有会话
  const saved = sessionStorage.getItem('demo_candidate');
  if (saved) {
    currentUser = JSON.parse(saved);
    showMainApp();
  }
});

// ==================== 登录 ====================

async function loadDemoAccounts() {
  try {
    const resp = await fetch(`${API_BASE}/demo-api/demo-accounts`);
    const json = await resp.json();
    if (json.code === 0) {
      renderDemoAccounts(json.data);
    }
  } catch (e) {
    console.error('加载演示账号失败:', e);
  }
}

function renderDemoAccounts(accounts) {
  const container = document.getElementById('demo-accounts-list');
  container.innerHTML = accounts.map(acc => `
    <div class="demo-account-item" onclick="selectAccount('${acc.candidateId}')">
      <span class="acc-id">${acc.candidateId}</span>
      <span class="acc-name">${acc.name}</span>
      <span class="acc-score">总分 ${acc.totalScore}</span>
    </div>
  `).join('');
}

function selectAccount(id) {
  document.getElementById('login-candidate-id').value = id;
  demoLogin();
}

async function demoLogin() {
  const candidateId = document.getElementById('login-candidate-id').value.trim();
  if (!candidateId) { showToast('请输入考生编号'); return; }

  try {
    const resp = await fetch(`${API_BASE}/demo-api/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ candidateId })
    });
    const json = await resp.json();
    if (json.code === 0) {
      currentUser = json.data;
      sessionStorage.setItem('demo_candidate', JSON.stringify(currentUser));
      showMainApp();
    } else {
      showToast(json.message || '登录失败');
    }
  } catch (e) {
    showToast('网络错误，请重试');
  }
}

function logout() {
  sessionStorage.removeItem('demo_candidate');
  currentUser = null;
  if (pollingTimer) clearInterval(pollingTimer);
  document.getElementById('page-login').style.display = 'flex';
  document.getElementById('page-main').style.display = 'none';
}

// ==================== 主应用 ====================

function showMainApp() {
  document.getElementById('page-login').style.display = 'none';
  document.getElementById('page-main').style.display = 'block';

  document.getElementById('candidate-name').textContent = currentUser.name;
  document.getElementById('candidate-id').textContent = currentUser.candidateId;

  loadAllData();
  startPolling();
}

async function loadAllData() {
  await Promise.all([loadPushRecords(), loadNotifications()]);
  renderDashboard();
  renderNotifications();
  renderHistory();
  renderProfile();
}

function startPolling() {
  // 每 15 秒轮询通知，模拟实时推送
  pollingTimer = setInterval(async () => {
    const prev = currentNotifications.length;
    await loadNotifications();
    const curr = currentNotifications.length;
    if (curr > prev) {
      renderNotifications();
      renderDashboard();
      showToast('📬 您有新的消息通知');
    }
    updateUnreadBadge();
  }, 15000);
}

// ==================== 导航 ====================

function showPage(page) {
  document.querySelectorAll('.view').forEach(v => v.style.display = 'none');
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));

  document.getElementById(`view-${page}`).style.display = 'block';
  document.querySelector(`.nav-item[data-page="${page}"]`).classList.add('active');
}

// ==================== 数据加载 ====================

async function loadPushRecords() {
  try {
    const resp = await fetch(`${API_BASE}/demo-api/push-records?candidateId=${currentUser.candidateId}`);
    const json = await resp.json();
    if (json.code === 0) currentPushRecords = json.data;
  } catch (e) { console.error(e); }
}

async function loadNotifications() {
  try {
    const resp = await fetch(`${API_BASE}/demo-api/notifications?candidateId=${currentUser.candidateId}`);
    const json = await resp.json();
    if (json.code === 0) {
      currentNotifications = json.data.notifications;
      updateUnreadBadge();
    }
  } catch (e) { console.error(e); }
}

function updateUnreadBadge() {
  const unread = currentNotifications.filter(n => !n.read).length;
  const badge = document.getElementById('nav-unread-badge');
  if (badge) {
    badge.textContent = unread;
    badge.style.display = unread > 0 ? 'inline' : 'none';
  }
}

// ==================== 仪表盘 ====================

function renderDashboard() {
  const total = currentPushRecords.length;
  const pending = currentPushRecords.filter(r => r.status === 'PENDING').length;
  const admitted = currentPushRecords.filter(r => ['ADMITTED', 'CONFIRMED', 'MATERIAL_SENT'].includes(r.status)).length;
  const confirmed = currentPushRecords.filter(r => r.status === 'CONFIRMED' || r.status === 'MATERIAL_SENT').length;

  document.getElementById('kpi-total').textContent = total;
  document.getElementById('kpi-pending').textContent = pending;
  document.getElementById('kpi-admitted').textContent = admitted;
  document.getElementById('kpi-confirmed').textContent = confirmed;

  const list = document.getElementById('push-records-list');
  if (currentPushRecords.length === 0) {
    list.innerHTML = renderEmptyState('暂无推送记录', '您还没有向任何院校推送成绩', '📨');
    return;
  }

  list.innerHTML = currentPushRecords.map(record => renderPushCard(record)).join('');
}

function renderPushCard(record) {
  const roundText = record.round === 1 ? '首轮' : `第${record.round}轮补录`;
  const isConditional = record.status === 'CONDITIONAL';

  let countdownHtml = '';
  if (isConditional && record.remark) {
    const deadlineStr = extractDeadline(record.remark);
    if (deadlineStr) {
      countdownHtml = renderCountdown(deadlineStr);
    }
  }

  let remarkHtml = '';
  if (isConditional && record.remark) {
    remarkHtml = `<div class="push-card-remark"><strong>📋 录取条件：</strong>${record.remark}</div>`;
  } else if (record.status === 'ADMITTED' && record.remark) {
    remarkHtml = `<div class="push-card-remark"><strong>📝 院校备注：</strong>${record.remark}</div>`;
  }

  let actionsHtml = '';
  if (record.status === 'ADMITTED') {
    actionsHtml = `
      <button class="btn btn-success btn-sm" onclick="openConfirmModal('${record.schoolId}', '${record.schoolName}', '${record.admissionMajor || record.majorName}')">✅ 确认录取</button>
      <button class="btn btn-outline btn-sm" onclick="openGiveUpModal('${record.schoolId}')">放弃</button>
    `;
  } else if (record.status === 'CONFIRMED') {
    actionsHtml = `
      <button class="btn btn-primary btn-sm" onclick="openMaterialModal('${record.schoolId}')">📦 确认已寄送材料</button>
    `;
  } else if (record.status === 'MATERIAL_SENT') {
    actionsHtml = `<span style="font-size:0.8rem;color:var(--text-secondary)">✅ 材料已寄送，等待院校报到确认</span>`;
  }

  return `
    <div class="push-card">
      <div class="push-card-header">
        <div>
          <div class="push-card-school">${record.schoolName}</div>
          <div class="push-card-round">${roundText} · 推送时间 ${formatTime(record.pushedAt)}</div>
        </div>
        <span class="badge badge-${getBadgeClass(record.status)}">${record.statusText}</span>
      </div>
      <div class="push-card-body">
        ${countdownHtml}
        ${remarkHtml}
        <div class="push-card-meta">
          <div class="push-card-meta-item">
            <span class="push-card-meta-label">意向专业</span>
            <span class="push-card-meta-value">${record.majorName || '—'}</span>
          </div>
          ${record.admissionMajor ? `
          <div class="push-card-meta-item">
            <span class="push-card-meta-label">录取专业</span>
            <span class="push-card-meta-value" style="color:var(--success);font-weight:700">${record.admissionMajor}</span>
          </div>
          ` : ''}
        </div>
        ${actionsHtml ? `<div class="push-card-actions">${actionsHtml}</div>` : ''}
      </div>
    </div>
  `;
}

function renderCountdown(deadlineStr) {
  const deadline = new Date(deadlineStr);
  const now = new Date();
  const diff = deadline - now;

  if (diff <= 0) {
    return `<div class="countdown">
      <span class="countdown-icon">⏰</span>
      <span class="countdown-label">条件截止时间：</span>
      <span class="countdown-expired">已到期！请尽快联系院校</span>
    </div>`;
  }

  const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));

  return `<div class="countdown">
    <span class="countdown-icon">⏰</span>
    <span class="countdown-label">条件截止：</span>
    <span class="countdown-time">${days > 0 ? days + '天' : ''}${hours}小时</span>
    <span style="color:var(--text-secondary)">（${deadlineStr}）</span>
  </div>`;
}

function extractDeadline(text) {
  // 简单提取：寻找 YYYY-MM-DD 格式
  const match = text.match(/\d{4}-\d{2}-\d{2}/);
  return match ? match[0] : null;
}

function getBadgeClass(status) {
  return {
    'PENDING': 'warning',
    'CONDITIONAL': 'info',
    'ADMITTED': 'success',
    'CONFIRMED': 'primary',
    'REJECTED': 'danger',
    'INVALIDATED': 'danger',
    'MATERIAL_SENT': 'primary',
  }[status] || 'default';
}

// ==================== 通知 ====================

function renderNotifications() {
  const list = document.getElementById('notifications-list');
  updateUnreadBadge();

  if (currentNotifications.length === 0) {
    list.innerHTML = renderEmptyState('暂无通知', '各院校的录取结果将在这里显示', '🔔');
    return;
  }

  list.innerHTML = currentNotifications.map(n => `
    <div class="notif-card ${n.read ? '' : 'unread'} ${n.typeClass}" onclick="handleNotifClick('${n.id}', '${n.type}')">
      <div class="notif-icon">${n.typeIcon}</div>
      <div class="notif-content">
        <div class="notif-title">${n.title}</div>
        <div class="notif-body">${n.content}</div>
        <div class="notif-time">${formatTime(n.createdAt)}</div>
      </div>
      ${n.read ? '' : '<div class="notif-unread-dot"></div>'}
    </div>
  `).join('');
}

async function handleNotifClick(notifId, type) {
  await fetch(`${API_BASE}/demo-api/notifications/${notifId}/read?candidateId=${currentUser.candidateId}`, { method: 'PATCH' });
  // 更新本地状态
  const n = currentNotifications.find(x => x.id === notifId);
  if (n) n.read = true;
  renderNotifications();
}

async function markAllRead() {
  await fetch(`${API_BASE}/demo-api/notifications/read-all?candidateId=${currentUser.candidateId}`, { method: 'POST' });
  currentNotifications.forEach(n => n.read = true);
  renderNotifications();
  showToast('已全部标为已读');
}

// ==================== 历史 ====================

function renderHistory() {
  const list = document.getElementById('history-list');
  if (currentPushRecords.length === 0) {
    list.innerHTML = renderEmptyState('暂无记录', '', '📋');
    return;
  }

  list.innerHTML = `
    <table class="history-table">
      <thead>
        <tr>
          <th>院校名称</th>
          <th>意向专业</th>
          <th>录取专业</th>
          <th>当前状态</th>
          <th>推送时间</th>
          <th>轮次</th>
        </tr>
      </thead>
      <tbody>
        ${currentPushRecords.map(r => `
          <tr>
            <td><strong>${r.schoolName}</strong></td>
            <td>${r.majorName || '—'}</td>
            <td>${r.admissionMajor || '—'}</td>
            <td><span class="badge badge-${getBadgeClass(r.status)}">${r.statusText}</span></td>
            <td>${formatTime(r.pushedAt)}</td>
            <td>${r.round === 1 ? '首轮' : `第${r.round}轮`}</td>
          </tr>
        `).join('')}
      </tbody>
    </table>
  `;
}

// ==================== 个人资料 ====================

function renderProfile() {
  document.getElementById('profile-name').textContent = currentUser.name;
  document.getElementById('profile-id').textContent = currentUser.candidateId;

  const scoreEntries = Object.entries(currentUser.subjectScores || {}).map(
    ([k, v]) => `<div class="profile-row"><div class="profile-row-label">${k}</div><div class="profile-row-value">${v} 分</div></div>`
  ).join('');

  const natMap = { 'CN': '中国', 'US': '美国', 'PK': '巴基斯坦', 'JP': '日本', 'KR': '韩国' };

  document.getElementById('profile-details').innerHTML = `
    <div class="profile-row"><div class="profile-row-label">考生编号</div><div class="profile-row-value">${currentUser.candidateId}</div></div>
    <div class="profile-row"><div class="profile-row-label">姓名</div><div class="profile-row-value">${currentUser.name}</div></div>
    <div class="profile-row"><div class="profile-row-label">英文名</div><div class="profile-row-value">${currentUser.englishName || '—'}</div></div>
    <div class="profile-row"><div class="profile-row-label">国籍</div><div class="profile-row-value">${natMap[currentUser.nationality] || currentUser.nationality}</div></div>
    <div class="profile-row"><div class="profile-row-label">邮箱</div><div class="profile-row-value">${currentUser.email || '—'}</div></div>
    <div class="profile-row"><div class="profile-row-label">总分</div><div class="profile-row-value" style="font-weight:800;font-size:1.1rem;color:var(--primary)">${currentUser.totalScore}</div></div>
    ${scoreEntries}
    <div class="profile-row"><div class="profile-row-label">意向方向</div><div class="profile-row-value">${currentUser.intention || '—'}</div></div>
  `;
}

// ==================== 模态框：确认录取 ====================

function openConfirmModal(schoolId, schoolName, majorName) {
  modalContext.schoolId = schoolId;

  const otherAdmitted = currentPushRecords.filter(
    r => r.status === 'ADMITTED' && r.schoolId !== schoolId
  );

  let riskHtml = '';
  if (otherAdmitted.length > 0) {
    const otherNames = otherAdmitted.map(r => r.schoolName).join('、');
    riskHtml = `
      <div class="risk-warning">
        <strong>⚠️ 重要提示：录取互斥规则</strong>
        确认接受 <strong>${schoolName}</strong> 的录取后，您在 ${otherNames} 的录取通知将自动失效，名额将被释放。
        此操作不可撤销。
      </div>
    `;
  }

  document.getElementById('modal-confirm-body').innerHTML = `
    <p style="margin-bottom:1rem;font-size:0.95rem;">
      您已收到 <strong>${schoolName}</strong> 的录取通知，专业：<strong>${majorName}</strong>。
    </p>
    ${riskHtml}
    <p style="font-size:0.875rem;color:var(--text-secondary)">
      确认后请按院校要求准备并寄送入学材料。材料寄送后，请在本页面点击"确认已寄送材料"。
    </p>
  `;

  document.getElementById('btn-confirm').style.display = 'inline-flex';
  document.getElementById('btn-give-up').style.display = 'none';
  document.getElementById('modal-confirm').style.display = 'flex';
}

function openGiveUpModal(schoolId) {
  modalContext.schoolId = schoolId;
  const record = currentPushRecords.find(r => r.schoolId === schoolId);

  document.getElementById('modal-confirm-body').innerHTML = `
    <div class="risk-warning">
      <strong>⚠️ 确定要放弃该录取吗？</strong>
      放弃后，该院校可将录取名额分配给其他考生。
    </div>
    <p style="font-size:0.875rem;color:var(--text-secondary);margin-top:0.75rem;">
      您可以重新参加补录轮次向该院校重新推送成绩。
    </p>
  `;

  document.getElementById('btn-confirm').style.display = 'none';
  document.getElementById('btn-give-up').style.display = 'inline-flex';
  document.getElementById('modal-confirm').style.display = 'flex';
}

async function confirmAdmission() {
  const schoolId = modalContext.schoolId;
  closeModal('confirm');

  try {
    const resp = await fetch(`${API_BASE}/demo-api/confirm`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ candidateId: currentUser.candidateId, schoolId })
    });
    const json = await resp.json();

    if (json.code === 0) {
      const confirmed = json.data.confirmedSchool;
      const invalidated = json.data.invalidatedSchools || [];

      let resultHtml = `
        <div style="text-align:center;margin-bottom:1.25rem;">
          <div style="font-size:3rem;margin-bottom:0.5rem;">🎉</div>
          <div style="font-size:1.1rem;font-weight:700;color:var(--success)">录取已确认！</div>
          <div style="font-size:0.9rem;color:var(--text-secondary);margin-top:0.25rem;">
            您已确认接受 <strong>${confirmed ? confirmed.name : ''}</strong> 的录取
          </div>
        </div>
      `;

      if (invalidated.length > 0) {
        resultHtml += `
          <div style="background:var(--danger-bg);border-radius:var(--radius);padding:0.875rem;margin-bottom:0.75rem;">
            <div style="font-weight:700;color:var(--danger);margin-bottom:0.3rem;">⚠️ 以下院校录取已自动失效</div>
            ${invalidated.map(r => `<div style="font-size:0.85rem;color:#721c24;">• ${r.schoolName} — ${r.invalidatedReason || '名额已释放'}</div>`).join('')}
          </div>
        `;
      }

      resultHtml += `<p style="font-size:0.875rem;color:var(--text-secondary)">请准备并寄送入学材料至院校，寄送后在下方点击"确认已寄送材料"。</p>`;

      document.getElementById('result-title').textContent = '✅ 确认成功';
      document.getElementById('modal-result-body').innerHTML = resultHtml;
      document.getElementById('modal-result').style.display = 'flex';

      // 刷新数据
      await loadAllData();

    } else {
      showToast(json.message || '确认失败');
    }
  } catch (e) {
    showToast('网络错误，请重试');
  }
}

async function giveUpAdmission() {
  const schoolId = modalContext.schoolId;
  closeModal('confirm');

  try {
    const resp = await fetch(`${API_BASE}/demo-api/give-up`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ candidateId: currentUser.candidateId, schoolId })
    });
    const json = await resp.json();

    if (json.code === 0) {
      showToast('已放弃该录取');
      await loadAllData();
    } else {
      showToast(json.message || '操作失败');
    }
  } catch (e) {
    showToast('网络错误');
  }
}

// ==================== 模态框：寄送材料 ====================

function openMaterialModal(schoolId) {
  modalContext.schoolId = schoolId;
  const record = currentPushRecords.find(r => r.schoolId === schoolId);
  document.getElementById('material-tracking-no').value = '';
  document.getElementById('material-remark').value = '';
  document.getElementById('modal-material').style.display = 'flex';
}

async function submitMaterialSent() {
  const schoolId = modalContext.schoolId;
  const trackingNo = document.getElementById('material-tracking-no').value.trim();
  const remark = document.getElementById('material-remark').value.trim();

  try {
    const resp = await fetch(`${API_BASE}/demo-api/material-sent`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ candidateId: currentUser.candidateId, schoolId, trackingNo, remark })
    });
    const json = await resp.json();

    if (json.code === 0) {
      closeModal('material');
      showToast('✅ 材料寄送信息已记录');
      await loadAllData();
    } else {
      showToast(json.message || '操作失败');
    }
  } catch (e) {
    showToast('网络错误');
  }
}

// ==================== 模态框通用 ====================

function closeModal(type) {
  document.getElementById(`modal-${type}`).style.display = 'none';
}

// ==================== 工具函数 ====================

function formatTime(isoString) {
  if (!isoString) return '—';
  const d = new Date(isoString);
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function pad(n) { return n < 10 ? '0' + n : n; }

function showToast(msg) {
  const toast = document.getElementById('toast');
  toast.textContent = msg;
  toast.style.display = 'block';
  setTimeout(() => toast.style.display = 'none', 3000);
}

function renderEmptyState(title, desc, icon) {
  return `
    <div class="empty-state">
      <div class="empty-icon">${icon}</div>
      <div class="empty-title">${title}</div>
      <div class="empty-desc">${desc}</div>
    </div>
  `;
}
