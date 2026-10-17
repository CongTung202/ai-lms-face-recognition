/**
 * EduFace LMS Admin - Common Utilities & Shared Navigation State
 */
const API_BASE = (window.location.protocol === 'file:' || !window.location.origin || window.location.origin === 'null')
    ? 'http://localhost:8080'
    : window.location.origin;

// Auto-run common bootstrap when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    initSystemClock();
    checkSystemHealth();
    loadSidebarBadgeCounts();
});

function initSystemClock() {
    const clock = document.getElementById('systemClock');
    if (!clock) return;
    const updateTime = () => {
        const now = new Date();
        clock.textContent = now.toLocaleTimeString('vi-VN');
    };
    updateTime();
    setInterval(updateTime, 1000);
}

async function checkSystemHealth() {
    const healthPill = document.getElementById('backendHealthPill');
    const healthText = document.getElementById('backendHealthText');
    if (!healthPill || !healthText) return;
    try {
        const res = await fetch(`${API_BASE}/api/health`);
        if (res.ok) {
            const data = await res.json();
            healthPill.className = 'hidden sm:flex items-center px-2.5 py-1 rounded-full text-xs font-medium border bg-emerald-50 text-emerald-700 border-emerald-200';
            healthText.textContent = `Backend: Sẵn sàng (${data.mysql_total_users || 0} users)`;
        }
    } catch (e) {
        healthPill.className = 'hidden sm:flex items-center px-2.5 py-1 rounded-full text-xs font-medium border bg-amber-50 text-amber-700 border-amber-200';
        healthText.textContent = 'Backend: Ngoại tuyến';
    }
}

async function loadSidebarBadgeCounts() {
    try {
        const res = await fetch(`${API_BASE}/api/admin/stats`);
        if (res.ok) {
            const result = await res.json();
            const s = result.data || {};
            const coursesCountEl = document.getElementById('sidebarCoursesCount');
            if (coursesCountEl) coursesCountEl.textContent = s.totalCourses || 0;

            const studentsCountEl = document.getElementById('sidebarStudentsCount');
            if (studentsCountEl) studentsCountEl.textContent = s.totalUsers || 0;

            const classesCountEl = document.getElementById('sidebarClassesCount');
            if (classesCountEl) classesCountEl.textContent = s.totalClasses || 0;
        }
    } catch (e) {
        console.warn('Lỗi tải thống kê sidebar:', e);
    }
}

function toggleSidebar() {
    const sb = document.getElementById('mainSidebar');
    if (sb) sb.classList.toggle('hidden');
}

function showToast(msg) {
    let t = document.getElementById('toastNotification');
    let m = document.getElementById('toastMessage');
    if (!t) {
        t = document.createElement('div');
        t.id = 'toastNotification';
        t.className = 'fixed top-5 right-5 z-50 bg-slate-900 text-white px-4 py-3 rounded-xl shadow-lg border border-slate-700 text-sm font-medium flex items-center space-x-2 transition-all';
        t.innerHTML = '<span id="toastMessage"></span>';
        document.body.appendChild(t);
        m = document.getElementById('toastMessage');
    }
    m.textContent = msg;
    t.classList.remove('hidden');
    setTimeout(() => {
        t.classList.add('hidden');
    }, 3200);
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function escapeJs(str) {
    if (!str) return '';
    return String(str)
        .replace(/\\/g, '\\\\')
        .replace(/'/g, "\\'")
        .replace(/"/g, '\\"')
        .replace(/\n/g, '\\n')
        .replace(/\r/g, '');
}

function handleGlobalSearch(e) {
    if (e.key === 'Enter') {
        showToast(`Tìm kiếm: ${e.target.value}`);
    }
}
