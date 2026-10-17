/**
 * EduFace LMS - Modular Admin Component Loader & Shared Layout
 * Injects reusable Header and Sidebar components, manages global UI state
 */

const API_BASE = (window.location.protocol === 'file:' || !window.location.origin || window.location.origin === 'null')
    ? 'http://localhost:8080'
    : window.location.origin;

function getAdminRoot() {
    if (window.location.protocol === 'file:') {
        // Find relative path to admin root if opened as file
        const p = window.location.pathname.replace(/\\/g, '/');
        const idx = p.indexOf('/admin/');
        if (idx !== -1) return p.substring(0, idx + 7);
    }
    return '/admin/';
}

const ADMIN_ROOT = getAdminRoot();

function getActiveNavKey() {
    if (window.__ACTIVE_NAV__) return window.__ACTIVE_NAV__;
    const bodyNav = document.body.getAttribute('data-nav');
    if (bodyNav) return bodyNav;
    const p = window.location.pathname.toLowerCase();
    if (p.includes('/courses')) return 'courses';
    if (p.includes('/students')) return 'students';
    if (p.includes('/faculties')) return 'faculties';
    if (p.includes('/classes')) return 'classes';
    if (p.includes('/profile') || p.includes('/settings')) return 'profile';
    return 'dashboard';
}

function renderAdminSidebar(activeKey) {
    const el = document.getElementById('admin-sidebar') || document.getElementById('admin-sidebar-container');
    if (!el) return;

    const navItems = [
        {
            key: 'dashboard',
            label: 'Bảng điều khiển',
            href: `${ADMIN_ROOT}index.html`,
            icon: `<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"></path></svg>`
        },
        {
            key: 'courses',
            label: 'Quản lý Khóa học',
            href: `${ADMIN_ROOT}courses/list.html`,
            badgeId: 'sidebarCoursesCount',
            icon: `<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"></path></svg>`
        },
        {
            key: 'students',
            label: 'Quản lý Sinh viên',
            href: `${ADMIN_ROOT}students/list.html`,
            badgeId: 'sidebarStudentsCount',
            icon: `<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path></svg>`
        },
        {
            key: 'faculties',
            label: 'Quản lý Khoa đào tạo',
            href: `${ADMIN_ROOT}faculties/list.html`,
            badgeId: 'sidebarFacultiesCount',
            icon: `<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"></path></svg>`
        },
        {
            key: 'classes',
            label: 'Quản lý Lớp học',
            href: `${ADMIN_ROOT}classes/list.html`,
            badgeId: 'sidebarClassesCount',
            icon: `<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 14v3m4-3v3m4-3v3M3 21h18M3 10h18M3 7l9-4 9 4M4 10h16v11H4V10z"></path></svg>`
        }
    ];

    const sysItems = [
        {
            key: 'profile',
            label: 'Cài đặt & eKYC ArcFace',
            href: `${ADMIN_ROOT}profile.html`,
            icon: `<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"></path></svg>`
        }
    ];

    const renderNavGroup = (items) => items.map(item => {
        const isActive = item.key === activeKey;
        const activeClass = isActive
            ? 'bg-blue-50 text-blue-600 font-semibold border-r-4 border-blue-600 shadow-sm'
            : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900 font-medium';
        const iconColor = isActive ? 'text-blue-600' : 'text-slate-400';
        const badge = item.badgeId
            ? `<span id="${item.badgeId}" class="text-xs ${isActive ? 'bg-blue-100 text-blue-700' : 'bg-slate-100 text-slate-600'} px-2 py-0.5 rounded-full font-bold">--</span>`
            : '';

        return `
            <a href="${item.href}" data-nav-item="${item.key}"
                class="flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm transition-all duration-150 ${activeClass}">
                <div class="flex items-center space-x-3">
                    <span class="${iconColor}">${item.icon}</span>
                    <span>${item.label}</span>
                </div>
                ${badge}
            </a>
        `;
    }).join('');

    el.innerHTML = `
        <aside id="mainSidebar" class="w-64 bg-white border-r border-slate-200 flex flex-col justify-between shrink-0 min-h-screen transition-all duration-300">
            <div class="py-4">
                <div class="px-5 mb-4 flex items-center justify-between">
                    <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Điều Hướng Quản Trị</span>
                </div>
                <nav class="space-y-1 px-3">
                    ${renderNavGroup(navItems)}
                </nav>

                <div class="pt-6 px-5 mb-2">
                    <span class="text-xs font-bold uppercase tracking-wider text-slate-400">Hệ Thống</span>
                </div>
                <nav class="space-y-1 px-3">
                    ${renderNavGroup(sysItems)}
                </nav>
            </div>

            <div class="p-4 border-t border-slate-200">
                <div class="bg-slate-50 border border-slate-200 rounded-xl p-3">
                    <div class="flex items-center justify-between mb-2">
                        <span class="text-[11px] font-bold text-slate-700 uppercase tracking-wider">Vi Dịch Vụ</span>
                        <span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                    </div>
                    <div class="space-y-1.5 text-xs text-slate-600">
                        <div class="flex items-center justify-between">
                            <span>Face AI (Port 8001):</span>
                            <span class="font-semibold text-emerald-600">ArcFace 512D</span>
                        </div>
                        <div class="flex items-center justify-between">
                            <span>LMS Core (Port 8080):</span>
                            <span class="font-semibold text-blue-600">Hoạt động</span>
                        </div>
                        <div class="flex items-center justify-between">
                            <span>Cloudinary CDN:</span>
                            <span class="font-semibold text-purple-600">HLS / MP4</span>
                        </div>
                    </div>
                </div>
            </div>
        </aside>
    `;
}

function renderAdminHeader() {
    const el = document.getElementById('admin-header') || document.getElementById('admin-header-container');
    if (!el) return;

    el.innerHTML = `
        <header class="bg-white border-b border-slate-200 sticky top-0 z-40 px-4 lg:px-6 py-2.5 flex items-center justify-between shadow-sm">
            <div class="flex items-center space-x-3">
                <button id="sidebarToggleBtn" onclick="toggleSidebar()"
                    class="p-2 rounded-lg hover:bg-slate-100 text-slate-600 focus:outline-none transition"
                    title="Đóng / Mở thanh điều hướng">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path>
                    </svg>
                </button>
                <a href="${ADMIN_ROOT}index.html" class="flex items-center space-x-2">
                    <div class="w-8 h-8 rounded-lg bg-gradient-to-tr from-blue-600 to-indigo-600 flex items-center justify-center text-white font-black text-sm shadow-sm">
                        E
                    </div>
                    <div class="hidden sm:block">
                        <span class="text-base font-bold text-slate-800 tracking-tight">EduFace</span>
                        <span class="text-[11px] font-semibold text-blue-600 bg-blue-50 px-1.5 py-0.5 rounded ml-1 border border-blue-100">Admin</span>
                    </div>
                </a>
            </div>

            <!-- Center Search -->
            <div class="hidden md:flex items-center space-x-4 flex-1 max-w-md mx-6">
                <div class="relative w-full">
                    <input type="text" id="globalHeaderSearch"
                        placeholder="Tìm kiếm nhanh..."
                        onkeydown="handleGlobalSearch(event)"
                        class="w-full bg-slate-50 border border-slate-200 text-xs rounded-lg pl-9 pr-3 py-1.5 text-slate-700 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition">
                    <svg class="w-4 h-4 text-slate-400 absolute left-3 top-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                    </svg>
                </div>
            </div>

            <!-- Right Controls -->
            <div class="flex items-center space-x-3">
                <div class="hidden lg:flex items-center px-2.5 py-1 rounded-md bg-slate-100 text-slate-700 text-xs font-mono font-medium">
                    <svg class="w-3.5 h-3.5 mr-1 text-slate-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                    </svg>
                    <span id="systemClock">--:--:--</span>
                </div>

                <div id="backendHealthPill" class="hidden sm:flex items-center px-2.5 py-1 rounded-full text-xs font-medium border bg-emerald-50 text-emerald-700 border-emerald-200">
                    <span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse mr-1.5"></span>
                    <span id="backendHealthText">Backend: Sẵn sàng</span>
                </div>

                <a href="${ADMIN_ROOT}profile.html" class="flex items-center space-x-2 p-1 pl-2 rounded-full hover:bg-slate-100 transition border border-transparent hover:border-slate-200">
                    <div class="text-right hidden sm:block">
                        <div class="text-xs font-semibold text-slate-800 leading-none">Nguyễn Công Tùng</div>
                        <div class="text-[10px] text-slate-500 font-medium leading-none mt-1">Quản trị viên cấp cao</div>
                    </div>
                    <img src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&auto=format&fit=crop&q=80"
                         alt="Admin Avatar" class="w-8 h-8 rounded-full object-cover ring-2 ring-blue-500/20">
                </a>
            </div>
        </header>
    `;
}

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

            const facultiesCountEl = document.getElementById('sidebarFacultiesCount');
            if (facultiesCountEl) facultiesCountEl.textContent = s.totalFaculties || 0;

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

// Auto-initialize components on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    const activeKey = getActiveNavKey();
    renderAdminSidebar(activeKey);
    renderAdminHeader();
    initSystemClock();
    checkSystemHealth();
    loadSidebarBadgeCounts();
});
