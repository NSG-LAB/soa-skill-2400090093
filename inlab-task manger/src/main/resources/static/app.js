/* ==========================================================================
   SOA TaskFlow Frontend Application Logic
   ========================================================================== */

const API_BASE = '/api/tasks';

// State Management
const state = {
    tasks: [],
    categories: [],
    summary: null,
    currentView: 'kanban', // 'kanban' or 'table'
    filters: {
        status: '',
        priority: '',
        category: '',
        search: ''
    },
    editingTaskId: null
};

// DOM Elements
const kanbanView = document.getElementById('kanbanView');
const tableView = document.getElementById('tableView');
const tableBody = document.getElementById('tableBody');
const tableEmptyState = document.getElementById('tableEmptyState');

const listTodo = document.getElementById('listTodo');
const listInProgress = document.getElementById('listInProgress');
const listCompleted = document.getElementById('listCompleted');

const countTodo = document.getElementById('countTodo');
const countInProgress = document.getElementById('countInProgress');
const countCompleted = document.getElementById('countCompleted');

const statTotal = document.getElementById('statTotal');
const statInProgress = document.getElementById('statInProgress');
const statCompleted = document.getElementById('statCompleted');
const statOverdue = document.getElementById('statOverdue');

const searchInput = document.getElementById('searchInput');
const searchClearBtn = document.getElementById('searchClearBtn');
const priorityFilterSelect = document.getElementById('priorityFilterSelect');
const categoryFilterSelect = document.getElementById('categoryFilterSelect');
const statusFilterPills = document.getElementById('statusFilterPills');

const taskModalOverlay = document.getElementById('taskModalOverlay');
const taskForm = document.getElementById('taskForm');
const modalTitle = document.getElementById('modalTitle');
const taskIdInput = document.getElementById('taskIdInput');
const taskTitleInput = document.getElementById('taskTitleInput');
const taskDescriptionInput = document.getElementById('taskDescriptionInput');
const taskStatusSelect = document.getElementById('taskStatusSelect');
const taskPrioritySelect = document.getElementById('taskPrioritySelect');
const taskCategoryInput = document.getElementById('taskCategoryInput');
const taskDueDateInput = document.getElementById('taskDueDateInput');
const titleError = document.getElementById('titleError');
const categoriesDatalist = document.getElementById('categoriesDatalist');

// Initialize Application
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    setupEventListeners();
    fetchSummary();
    fetchCategories();
    fetchTasks();
});

// Theme Management
function initTheme() {
    const savedTheme = localStorage.getItem('taskflow_theme') || 'dark';
    document.documentElement.setAttribute('data-theme', savedTheme);
}

function toggleTheme() {
    const currentTheme = document.documentElement.getAttribute('data-theme') || 'dark';
    const nextTheme = currentTheme === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', nextTheme);
    localStorage.setItem('taskflow_theme', nextTheme);
}

// Event Listeners Setup
function setupEventListeners() {
    document.getElementById('themeToggleBtn').addEventListener('click', toggleTheme);

    // View Switchers
    document.getElementById('kanbanViewBtn').addEventListener('click', () => switchView('kanban'));
    document.getElementById('listViewBtn').addEventListener('click', () => switchView('table'));

    // Modal Triggers
    document.getElementById('openCreateModalBtn').addEventListener('click', () => openTaskModal());
    document.getElementById('modalCloseBtn').addEventListener('click', closeTaskModal);
    document.getElementById('modalCancelBtn').addEventListener('click', closeTaskModal);
    taskForm.addEventListener('submit', handleTaskFormSubmit);

    // Filter controls
    let searchTimeout = null;
    searchInput.addEventListener('input', (e) => {
        searchClearBtn.style.display = e.target.value ? 'block' : 'none';
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
            state.filters.search = e.target.value.trim();
            fetchTasks();
        }, 250);
    });

    searchClearBtn.addEventListener('click', () => {
        searchInput.value = '';
        searchClearBtn.style.display = 'none';
        state.filters.search = '';
        fetchTasks();
    });

    statusFilterPills.addEventListener('click', (e) => {
        const pill = e.target.closest('.pill');
        if (!pill) return;
        statusFilterPills.querySelectorAll('.pill').forEach(p => p.classList.remove('active'));
        pill.classList.add('active');
        state.filters.status = pill.dataset.status;
        fetchTasks();
    });

    priorityFilterSelect.addEventListener('change', (e) => {
        state.filters.priority = e.target.value;
        fetchTasks();
    });

    categoryFilterSelect.addEventListener('change', (e) => {
        state.filters.category = e.target.value;
        fetchTasks();
    });

    // Stat cards click to filter
    document.querySelectorAll('.stat-card').forEach(card => {
        card.addEventListener('click', () => {
            const filterStatus = card.dataset.filterStatus;
            if (filterStatus) {
                const targetPill = statusFilterPills.querySelector(`[data-status="${filterStatus === 'ALL' ? '' : filterStatus}"]`);
                if (targetPill) targetPill.click();
            }
        });
    });

    // REST API Playground
    setupApiExplorer();
}

function switchView(view) {
    state.currentView = view;
    if (view === 'kanban') {
        kanbanView.classList.remove('hidden');
        tableView.classList.add('hidden');
        document.getElementById('kanbanViewBtn').classList.add('active');
        document.getElementById('listViewBtn').classList.remove('active');
    } else {
        kanbanView.classList.add('hidden');
        tableView.classList.remove('hidden');
        document.getElementById('kanbanViewBtn').classList.remove('active');
        document.getElementById('listViewBtn').classList.add('active');
    }
}

// ==========================================================================
// API Operations
// ==========================================================================

async function fetchTasks() {
    try {
        const params = new URLSearchParams();
        if (state.filters.status) params.append('status', state.filters.status);
        if (state.filters.priority) params.append('priority', state.filters.priority);
        if (state.filters.category) params.append('category', state.filters.category);
        if (state.filters.search) params.append('search', state.filters.search);

        const url = `${API_BASE}?${params.toString()}`;
        const res = await fetch(url);
        if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
        
        state.tasks = await res.json();
        renderTasks();
        setConnectionStatus(true);
    } catch (err) {
        console.error('Failed to load tasks:', err);
        setConnectionStatus(false);
        showToast('Error connecting to backend API', 'error');
    }
}

async function fetchSummary() {
    try {
        const res = await fetch(`${API_BASE}/summary`);
        if (!res.ok) throw new Error('Failed to fetch summary');
        const summary = await res.json();
        state.summary = summary;

        statTotal.textContent = summary.total;
        statInProgress.textContent = summary.inProgress;
        statCompleted.textContent = summary.completed;
        statOverdue.textContent = summary.overdue;

        const overdueCard = document.getElementById('overdueCard');
        if (summary.overdue > 0) {
            overdueCard.style.borderColor = 'rgba(239, 68, 68, 0.4)';
        } else {
            overdueCard.style.borderColor = 'var(--border-subtle)';
        }
    } catch (err) {
        console.warn('Could not update summary:', err);
    }
}

async function fetchCategories() {
    try {
        const res = await fetch(`${API_BASE}/categories`);
        if (!res.ok) return;
        state.categories = await res.json();
        populateCategoryOptions();
    } catch (err) {
        console.warn('Could not update categories:', err);
    }
}

function populateCategoryOptions() {
    const currentVal = categoryFilterSelect.value;
    categoryFilterSelect.innerHTML = '<option value="">All Categories</option>';
    categoriesDatalist.innerHTML = '';

    state.categories.forEach(cat => {
        const opt = document.createElement('option');
        opt.value = cat;
        opt.textContent = cat;
        if (cat === currentVal) opt.selected = true;
        categoryFilterSelect.appendChild(opt);

        const dataOpt = document.createElement('option');
        dataOpt.value = cat;
        categoriesDatalist.appendChild(dataOpt);
    });
}

function setConnectionStatus(online) {
    const badge = document.getElementById('serverStatusBadge');
    const dot = badge.querySelector('.status-dot');
    const label = badge.querySelector('.status-label');

    if (online) {
        dot.style.backgroundColor = 'var(--status-completed)';
        dot.style.boxShadow = '0 0 8px var(--status-completed)';
        label.textContent = 'API Connected';
    } else {
        dot.style.backgroundColor = '#EF4444';
        dot.style.boxShadow = '0 0 8px #EF4444';
        label.textContent = 'API Disconnected';
    }
}

// ==========================================================================
// Rendering
// ==========================================================================

function renderTasks() {
    renderKanban();
    renderTable();
}

function renderKanban() {
    listTodo.innerHTML = '';
    listInProgress.innerHTML = '';
    listCompleted.innerHTML = '';

    const todoTasks = state.tasks.filter(t => t.status === 'TODO');
    const inProgressTasks = state.tasks.filter(t => t.status === 'IN_PROGRESS');
    const completedTasks = state.tasks.filter(t => t.status === 'COMPLETED');

    countTodo.textContent = todoTasks.length;
    countInProgress.textContent = inProgressTasks.length;
    countCompleted.textContent = completedTasks.length;

    todoTasks.forEach(task => listTodo.appendChild(createTaskCardElement(task)));
    inProgressTasks.forEach(task => listInProgress.appendChild(createTaskCardElement(task)));
    completedTasks.forEach(task => listCompleted.appendChild(createTaskCardElement(task)));

    if (todoTasks.length === 0) listTodo.innerHTML = `<div class="table-empty"><p>No tasks</p></div>`;
    if (inProgressTasks.length === 0) listInProgress.innerHTML = `<div class="table-empty"><p>No tasks</p></div>`;
    if (completedTasks.length === 0) listCompleted.innerHTML = `<div class="table-empty"><p>No tasks</p></div>`;
}

function createTaskCardElement(task) {
    const card = document.createElement('div');
    card.className = 'task-card';
    card.dataset.id = task.id;

    const formattedDate = task.dueDate ? formatDate(task.dueDate) : null;
    const isOverdue = task.overdue;

    let stepperButtons = '';
    if (task.status === 'TODO') {
        stepperButtons = `<button class="step-btn" onclick="updateTaskStatus(${task.id}, 'IN_PROGRESS')">Start &rarr;</button>`;
    } else if (task.status === 'IN_PROGRESS') {
        stepperButtons = `
            <button class="step-btn" onclick="updateTaskStatus(${task.id}, 'TODO')">&larr; To Do</button>
            <button class="step-btn" onclick="updateTaskStatus(${task.id}, 'COMPLETED')">Complete &#10003;</button>
        `;
    } else if (task.status === 'COMPLETED') {
        stepperButtons = `<button class="step-btn" onclick="updateTaskStatus(${task.id}, 'IN_PROGRESS')">&#8635; Reopen</button>`;
    }

    card.innerHTML = `
        <div class="task-card-header">
            <span class="card-category-tag">${escapeHtml(task.category || 'General')}</span>
            <div class="card-actions">
                <button class="icon-btn" title="Edit" onclick="editTask(${task.id})">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                    </svg>
                </button>
                <button class="icon-btn danger" title="Delete" onclick="deleteTask(${task.id})">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="3 6 5 6 21 6"></polyline>
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                    </svg>
                </button>
            </div>
        </div>
        <div class="task-card-title">${escapeHtml(task.title)}</div>
        ${task.description ? `<div class="task-card-desc">${escapeHtml(task.description)}</div>` : ''}
        <div class="task-card-footer">
            <span class="priority-badge ${task.priority}">${task.priority}</span>
            ${formattedDate ? `<span class="due-date-badge ${isOverdue ? 'overdue' : ''}">📅 ${formattedDate}${isOverdue ? ' (Overdue)' : ''}</span>` : ''}
        </div>
        <div class="card-status-stepper">${stepperButtons}</div>
    `;

    return card;
}

function renderTable() {
    tableBody.innerHTML = '';
    if (state.tasks.length === 0) {
        tableEmptyState.classList.remove('hidden');
        return;
    }
    tableEmptyState.classList.add('hidden');

    state.tasks.forEach(task => {
        const tr = document.createElement('tr');
        const formattedDate = task.dueDate ? formatDate(task.dueDate) : '-';
        const isOverdue = task.overdue;

        tr.innerHTML = `
            <td>
                <input type="checkbox" ${task.status === 'COMPLETED' ? 'checked' : ''} 
                    onchange="updateTaskStatus(${task.id}, this.checked ? 'COMPLETED' : 'TODO')"
                    title="Toggle completion">
            </td>
            <td>
                <strong style="${task.status === 'COMPLETED' ? 'text-decoration: line-through; opacity: 0.6;' : ''}">${escapeHtml(task.title)}</strong>
                ${task.description ? `<p style="font-size: 0.75rem; color: var(--text-muted);">${escapeHtml(task.description)}</p>` : ''}
            </td>
            <td><span class="card-category-tag">${escapeHtml(task.category || 'General')}</span></td>
            <td><span class="priority-badge ${task.priority}">${task.priority}</span></td>
            <td><span class="status-badge ${task.status}">${task.status.replace('_', ' ')}</span></td>
            <td><span class="${isOverdue ? 'due-date-badge overdue' : ''}">${formattedDate}</span></td>
            <td style="text-align: right;">
                <button class="btn btn-sm btn-outline" onclick="editTask(${task.id})">Edit</button>
                <button class="btn btn-sm btn-secondary" onclick="deleteTask(${task.id})" style="color: #EF4444; margin-left: 6px;">Delete</button>
            </td>
        `;
        tableBody.appendChild(tr);
    });
}

// ==========================================================================
// CRUD Actions
// ==========================================================================

function openTaskModal(task = null) {
    titleError.textContent = '';
    if (task) {
        modalTitle.textContent = 'Edit Task';
        taskIdInput.value = task.id;
        taskTitleInput.value = task.title;
        taskDescriptionInput.value = task.description || '';
        taskStatusSelect.value = task.status;
        taskPrioritySelect.value = task.priority;
        taskCategoryInput.value = task.category || '';
        taskDueDateInput.value = task.dueDate || '';
        state.editingTaskId = task.id;
    } else {
        modalTitle.textContent = 'Create New Task';
        taskForm.reset();
        taskIdInput.value = '';
        taskStatusSelect.value = 'TODO';
        taskPrioritySelect.value = 'MEDIUM';
        state.editingTaskId = null;
    }
    taskModalOverlay.classList.remove('hidden');
    taskTitleInput.focus();
}

function closeTaskModal() {
    taskModalOverlay.classList.add('hidden');
    state.editingTaskId = null;
}

async function handleTaskFormSubmit(e) {
    e.preventDefault();
    titleError.textContent = '';

    const payload = {
        title: taskTitleInput.value.trim(),
        description: taskDescriptionInput.value.trim() || null,
        status: taskStatusSelect.value,
        priority: taskPrioritySelect.value,
        category: taskCategoryInput.value.trim() || 'General',
        dueDate: taskDueDateInput.value || null
    };

    if (payload.title.length < 3) {
        titleError.textContent = 'Title must be at least 3 characters long.';
        return;
    }

    try {
        const isEdit = !!state.editingTaskId;
        const url = isEdit ? `${API_BASE}/${state.editingTaskId}` : API_BASE;
        const method = isEdit ? 'PUT' : 'POST';

        const res = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!res.ok) {
            const errData = await res.json();
            throw new Error(errData.message || 'Operation failed');
        }

        closeTaskModal();
        showToast(isEdit ? 'Task updated successfully!' : 'Task created successfully!', 'success');
        await fetchTasks();
        await fetchSummary();
        await fetchCategories();
    } catch (err) {
        console.error(err);
        showToast(err.message, 'error');
    }
}

async function editTask(id) {
    try {
        const res = await fetch(`${API_BASE}/${id}`);
        if (!res.ok) throw new Error('Task not found');
        const task = await res.json();
        openTaskModal(task);
    } catch (err) {
        showToast('Failed to load task details', 'error');
    }
}

async function updateTaskStatus(id, newStatus) {
    try {
        const res = await fetch(`${API_BASE}/${id}/status`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: newStatus })
        });

        if (!res.ok) throw new Error('Failed to update task status');

        showToast(`Task moved to ${newStatus.replace('_', ' ')}`, 'info');
        await fetchTasks();
        await fetchSummary();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function deleteTask(id) {
    if (!confirm('Are you sure you want to delete this task?')) return;

    try {
        const res = await fetch(`${API_BASE}/${id}`, {
            method: 'DELETE'
        });

        if (!res.ok && res.status !== 204) throw new Error('Failed to delete task');

        showToast('Task deleted successfully', 'success');
        await fetchTasks();
        await fetchSummary();
        await fetchCategories();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==========================================================================
// REST API Explorer Feature
// ==========================================================================

function setupApiExplorer() {
    const apiBtn = document.getElementById('apiPlaygroundBtn');
    const apiOverlay = document.getElementById('apiModalOverlay');
    const apiCloseBtn = document.getElementById('apiModalCloseBtn');
    const executeBtn = document.getElementById('executeApiBtn');
    const epDisplayMethod = document.getElementById('activeEpMethod');
    const epDisplayUrl = document.getElementById('activeEpUrl');
    const requestBodyArea = document.getElementById('apiRequestBody');
    const requestWrapper = document.getElementById('apiRequestBodyWrapper');
    const responseStatus = document.getElementById('apiResponseStatus');
    const responseBody = document.getElementById('apiResponseBody');

    const epConfig = {
        getAll: { method: 'GET', url: '/api/tasks', hasBody: false },
        getSummary: { method: 'GET', url: '/api/tasks/summary', hasBody: false },
        postTask: {
            method: 'POST',
            url: '/api/tasks',
            hasBody: true,
            defaultBody: JSON.stringify({
                title: "New Integration Test Task",
                description: "Triggered from REST API Explorer",
                priority: "HIGH",
                status: "TODO",
                category: "Testing",
                dueDate: new Date().toISOString().split('T')[0]
            }, null, 2)
        },
        patchStatus: {
            method: 'PATCH',
            url: '/api/tasks/1/status',
            hasBody: true,
            defaultBody: JSON.stringify({ status: "COMPLETED" }, null, 2)
        },
        deleteTask: { method: 'DELETE', url: '/api/tasks/1', hasBody: false }
    };

    let selectedEpKey = 'getAll';

    apiBtn.addEventListener('click', () => {
        apiOverlay.classList.remove('hidden');
    });

    apiCloseBtn.addEventListener('click', () => {
        apiOverlay.classList.add('hidden');
    });

    document.querySelectorAll('.endpoint-item').forEach(item => {
        item.addEventListener('click', () => {
            document.querySelectorAll('.endpoint-item').forEach(i => i.classList.remove('active'));
            item.classList.add('active');
            selectedEpKey = item.dataset.endpoint;
            const conf = epConfig[selectedEpKey];

            epDisplayMethod.textContent = conf.method;
            epDisplayMethod.className = `http-badge ${conf.method.toLowerCase()}`;
            epDisplayUrl.textContent = window.location.origin + conf.url;

            if (conf.hasBody) {
                requestWrapper.style.display = 'flex';
                requestBodyArea.value = conf.defaultBody;
            } else {
                requestWrapper.style.display = 'none';
                requestBodyArea.value = '';
            }

            responseStatus.textContent = '-';
            responseBody.textContent = 'Click "Execute Request" to test endpoint...';
        });
    });

    executeBtn.addEventListener('click', async () => {
        const conf = epConfig[selectedEpKey];
        responseStatus.textContent = 'Executing...';
        responseBody.textContent = 'Sending request...';

        try {
            const options = {
                method: conf.method,
                headers: { 'Content-Type': 'application/json' }
            };

            if (conf.hasBody && requestBodyArea.value) {
                options.body = requestBodyArea.value;
            }

            const startTime = performance.now();
            const res = await fetch(conf.url, options);
            const duration = Math.round(performance.now() - startTime);

            responseStatus.textContent = `${res.status} ${res.statusText} (${duration}ms)`;
            responseStatus.style.color = res.ok ? '#10B981' : '#EF4444';

            if (res.status === 204) {
                responseBody.textContent = '204 No Content (Success)';
            } else {
                const data = await res.json();
                responseBody.textContent = JSON.stringify(data, null, 2);
            }

            // Refresh tasks in background if state-changing
            if (conf.method !== 'GET') {
                fetchTasks();
                fetchSummary();
            }
        } catch (err) {
            responseStatus.textContent = 'FAILED';
            responseStatus.style.color = '#EF4444';
            responseBody.textContent = String(err);
        }
    });
}

// ==========================================================================
// Utilities
// ==========================================================================

function formatDate(dateStr) {
    if (!dateStr) return '';
    const parts = dateStr.split('-');
    if (parts.length === 3) {
        const d = new Date(parts[0], parts[1] - 1, parts[2]);
        return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    }
    return dateStr;
}

function escapeHtml(text) {
    if (!text) return '';
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    let icon = 'ℹ️';
    if (type === 'success') icon = '✅';
    if (type === 'error') icon = '❌';

    toast.innerHTML = `<span>${icon}</span> <span>${escapeHtml(message)}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(10px)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}
