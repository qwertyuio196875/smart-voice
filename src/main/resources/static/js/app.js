const STATUS = {
    PENDING: { label: '待调度', cls: 'pill-pending' },
    RUNNING: { label: '进行中', cls: 'pill-running' },
    COMPLETED: { label: '已完成', cls: 'pill-completed' },
    CANCELLED: { label: '已取消', cls: 'pill-cancelled' },
    FAILED: { label: '已失败', cls: 'pill-failed' },
};

const POLL_INTERVAL = 10000;

const state = {
    tasks: [],
    stats: null,
    filter: 'ALL',
    firstLoad: true,
    loading: false,
};

const $ = (selector) => document.querySelector(selector);

const els = {
    skeleton: $('#skeleton'),
    table: $('#taskTable'),
    body: $('#taskBody'),
    emptyGlobal: $('#emptyGlobal'),
    emptyFilter: $('#emptyFilter'),
    errorBanner: $('#errorBanner'),
    errorText: $('#errorText'),
    resultCount: $('#resultCount'),
    updatedAt: $('#updatedAt'),
    toast: $('#toast'),
    dialog: $('#createDialog'),
    form: $('#createForm'),
    submit: $('#submitCreate'),
};

/* ---------- 数据加载 ---------- */

async function load({ silent = false } = {}) {
    if (state.loading) return;
    state.loading = true;
    if (!silent) document.body.classList.add('is-loading');

    try {
        const [tasks, stats] = await Promise.all([
            request('/api/tasks'),
            request('/api/stats'),
        ]);
        state.tasks = tasks;
        state.stats = stats;
        state.firstLoad = false;
        hideError();
        render();
        els.updatedAt.textContent = `刚刚更新 ${clock()}`;
    } catch (error) {
        showError(error.message || '数据加载失败');
        if (state.firstLoad) {
            els.skeleton.hidden = true;
            els.table.hidden = true;
        }
    } finally {
        state.loading = false;
        document.body.classList.remove('is-loading');
    }
}

async function request(url) {
    let response;
    try {
        response = await fetch(url, { headers: { Accept: 'application/json' } });
    } catch {
        throw new Error('无法连接服务器，请检查服务是否已启动');
    }
    if (!response.ok) {
        throw new Error(`服务暂时不可用（HTTP ${response.status}）`);
    }
    return response.json();
}

/* ---------- 渲染 ---------- */

function render() {
    els.skeleton.hidden = true;
    renderStats();
    renderTasks();
}

function renderStats() {
    const stats = state.stats || { total: 0, pending: 0, running: 0, totalDialed: 0 };
    $('#statTotal').textContent = stats.total;
    $('#statPending').textContent = stats.pending;
    $('#statRunning').textContent = stats.running;
    $('#statDialed').textContent = Number(stats.totalDialed).toLocaleString('zh-CN');
}

function renderTasks() {
    const tasks = state.tasks;
    const filtered = state.filter === 'ALL' ? tasks : tasks.filter((task) => task.status === state.filter);

    els.resultCount.textContent = state.filter === 'ALL'
        ? `共 ${tasks.length} 个任务`
        : `${filtered.length} 个任务 · 全部 ${tasks.length} 个`;

    const hasAny = tasks.length > 0;
    const hasRows = filtered.length > 0;

    els.emptyGlobal.hidden = hasAny;
    els.emptyFilter.hidden = !hasAny || hasRows;
    els.table.hidden = !hasRows;

    if (!hasRows) {
        els.body.textContent = '';
        return;
    }
    els.body.replaceChildren(...filtered.map(taskRow));
}

function taskRow(task) {
    const tr = document.createElement('tr');

    const nameCell = document.createElement('td');
    const name = document.createElement('span');
    name.className = 'task-name';
    name.textContent = task.name;
    const meta = document.createElement('span');
    meta.className = 'task-id';
    meta.textContent = `#${task.id} · 创建于 ${formatTime(task.createdAt)}`;
    nameCell.append(name, meta);

    const statusCell = document.createElement('td');
    const status = STATUS[task.status] || STATUS.PENDING;
    const pill = document.createElement('span');
    pill.className = `pill ${status.cls}`;
    pill.textContent = status.label;
    statusCell.append(pill);

    const contactCell = cell('num', Number(task.contactCount).toLocaleString('zh-CN'));
    const progressCell = document.createElement('td');
    progressCell.append(progressBar(task));

    const rateCell = document.createElement('td');
    rateCell.className = 'num rate' + (task.dialedCount === 0 ? ' rate-zero' : '');
    rateCell.textContent = task.dialedCount === 0
        ? '—'
        : `${Math.round((task.connectedCount / task.dialedCount) * 100)}%`;

    const concurrencyCell = cell('num', task.concurrency);
    const timeCell = cell('time', formatTime(task.scheduledAt));

    tr.append(nameCell, statusCell, contactCell, progressCell, rateCell, concurrencyCell, timeCell);
    return tr;
}

function cell(className, text) {
    const td = document.createElement('td');
    td.className = className;
    td.textContent = text;
    return td;
}

function progressBar(task) {
    const percent = task.contactCount === 0 ? 0 : Math.min(100, (task.dialedCount / task.contactCount) * 100);

    const wrap = document.createElement('div');
    wrap.className = 'progress';

    const track = document.createElement('span');
    track.className = 'progress-track';
    const fill = document.createElement('span');
    fill.className = 'progress-fill';
    fill.style.width = `${percent}%`;
    track.append(fill);

    const text = document.createElement('span');
    text.className = 'progress-text';
    text.textContent = `${Number(task.dialedCount).toLocaleString('zh-CN')} / ${Number(task.contactCount).toLocaleString('zh-CN')}`;

    wrap.append(track, text);
    return wrap;
}

/* ---------- 错误与提示 ---------- */

function showError(message) {
    els.errorText.textContent = message;
    els.errorBanner.hidden = false;
}

function hideError() {
    els.errorBanner.hidden = true;
}

let toastTimer = 0;

function toast(message) {
    els.toast.textContent = message;
    els.toast.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { els.toast.hidden = true; }, 2400);
}

/* ---------- 筛选 ---------- */

document.querySelectorAll('.chip').forEach((chip) => {
    chip.addEventListener('click', () => setFilter(chip.dataset.status));
});

function setFilter(status) {
    activateFilter(status);
    renderTasks();
}

function activateFilter(status) {
    state.filter = status;
    document.querySelectorAll('.chip').forEach((chip) => {
        chip.classList.toggle('is-active', chip.dataset.status === status);
    });
}

/* ---------- 新建任务 ---------- */

function openCreate() {
    els.form.reset();
    clearFieldErrors();
    $('#contactCount').value = 200;
    $('#concurrency').value = 10;
    $('#scheduledAt').value = defaultSchedule();
    els.dialog.showModal();
    $('#taskName').focus();
}

function defaultSchedule() {
    const date = new Date(Date.now() + 10 * 60 * 1000);
    const pad = (value) => String(value).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
        + `T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function clearFieldErrors() {
    document.querySelectorAll('.field-error').forEach((el) => { el.textContent = ''; });
    els.form.querySelectorAll('input').forEach((input) => input.removeAttribute('aria-invalid'));
}

function showFieldErrors(errors) {
    Object.entries(errors).forEach(([field, message]) => {
        const target = els.form.querySelector(`[data-error-for="${field}"]`);
        const input = els.form.querySelector(`[name="${field}"]`);
        if (target) target.textContent = message;
        if (input) input.setAttribute('aria-invalid', 'true');
    });
    const firstInvalid = els.form.querySelector('[aria-invalid="true"]');
    if (firstInvalid) firstInvalid.focus();
}

els.form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearFieldErrors();

    const payload = {
        name: $('#taskName').value.trim(),
        contactCount: Number($('#contactCount').value),
        concurrency: Number($('#concurrency').value),
        scheduledAt: $('#scheduledAt').value || null,
    };

    const errors = validate(payload);
    if (Object.keys(errors).length > 0) {
        showFieldErrors(errors);
        return;
    }

    els.submit.disabled = true;
    els.submit.textContent = '创建中…';
    try {
        const response = await fetch('/api/tasks', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
            body: JSON.stringify(payload),
        });
        if (!response.ok) {
            const body = await response.json().catch(() => ({}));
            if (body.errors && Object.keys(body.errors).length > 0) {
                showFieldErrors(body.errors);
                return;
            }
            throw new Error(body.message || '创建失败，请稍后重试');
        }
        els.dialog.close();
        toast('任务已创建');
        activateFilter('ALL');
        await load({ silent: true });
    } catch (error) {
        toast(error.message || '创建失败，请稍后重试');
    } finally {
        els.submit.disabled = false;
        els.submit.textContent = '创建任务';
    }
});

function validate(payload) {
    const errors = {};
    if (payload.name.length < 2 || payload.name.length > 40) {
        errors.name = '任务名称需 2–40 个字符';
    }
    if (!Number.isInteger(payload.contactCount) || payload.contactCount < 1 || payload.contactCount > 5000) {
        errors.contactCount = '名单数量需在 1–5000 之间';
    }
    if (!Number.isInteger(payload.concurrency) || payload.concurrency < 1 || payload.concurrency > 50) {
        errors.concurrency = '并发数需在 1–50 之间';
    } else if (payload.concurrency > payload.contactCount) {
        errors.concurrency = '并发数不能超过名单数量';
    }
    if (!payload.scheduledAt) {
        errors.scheduledAt = '请选择计划开始时间';
    }
    return errors;
}

/* ---------- 弹窗与事件 ---------- */

$('#newTaskBtn').addEventListener('click', openCreate);
$('#emptyCreateBtn').addEventListener('click', openCreate);
$('#clearFilterBtn').addEventListener('click', () => setFilter('ALL'));
$('#closeDialog').addEventListener('click', () => els.dialog.close());
$('#cancelCreate').addEventListener('click', () => els.dialog.close());
$('#refreshBtn').addEventListener('click', () => load());
$('#retryBtn').addEventListener('click', () => load());

els.dialog.addEventListener('click', (event) => {
    if (event.target === els.dialog) els.dialog.close();
});

/* ---------- 时间格式化 ---------- */

function formatTime(value) {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '—';
    const pad = (n) => String(n).padStart(2, '0');
    return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function clock() {
    const now = new Date();
    const pad = (n) => String(n).padStart(2, '0');
    return `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
}

/* ---------- 启动 ---------- */

load();
setInterval(() => {
    if (!document.hidden) load({ silent: true });
}, POLL_INTERVAL);
