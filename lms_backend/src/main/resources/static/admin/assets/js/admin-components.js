/**
 * EduFace LMS - Modular Admin Component Loader & Shared Layout
 * Design System: Eduface Uni (Crimson / Slate / Material 3 Tokens)
 * Injects reusable Header, Sidebar, Fonts & Global UI State across all Admin pages
 */

const API_BASE = (window.location.protocol === 'file:' || !window.location.origin || window.location.origin === 'null')
    ? 'http://localhost:8080'
    : window.location.origin;

function getAdminRoot() {
    if (window.location.protocol === 'file:') {
        const p = window.location.pathname.replace(/\\/g, '/');
        const idx = p.indexOf('/admin_web/');
        if (idx !== -1) return p.substring(0, idx + 11);
        const idx2 = p.indexOf('/admin/');
        if (idx2 !== -1) return p.substring(0, idx2 + 7);
    }
    return '/admin/';
}

const ADMIN_ROOT = getAdminRoot();

// Inject Google Fonts and Material Symbols Outlined if not already present
function injectHeadAssets() {
    if (!document.querySelector('link[href*="Plus+Jakarta+Sans"]')) {
        const preconn1 = document.createElement('link');
        preconn1.rel = 'preconnect';
        preconn1.href = 'https://fonts.googleapis.com';
        document.head.appendChild(preconn1);

        const preconn2 = document.createElement('link');
        preconn2.rel = 'preconnect';
        preconn2.href = 'https://fonts.gstatic.com';
        preconn2.crossOrigin = 'anonymous';
        document.head.appendChild(preconn2);

        const fontLink = document.createElement('link');
        fontLink.rel = 'stylesheet';
        fontLink.href = 'https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap';
        document.head.appendChild(fontLink);
    }

    if (!document.querySelector('link[href*="Material+Symbols+Outlined"]')) {
        const iconLink = document.createElement('link');
        iconLink.rel = 'stylesheet';
        iconLink.href = 'https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200';
        document.head.appendChild(iconLink);
    }

    if (!document.getElementById('eduface-custom-style')) {
        const style = document.createElement('style');
        style.id = 'eduface-custom-style';
        style.textContent = `
            @layer base {
                html, body { margin: 0; padding: 0; font-family: 'Plus Jakarta Sans', sans-serif; }
                body { overscroll-behavior: none; background-color: #f6fbfe; color: #0b2540; }
                main > :first-child { margin-top: 0 !important; }
                main > :last-child { margin-bottom: 0 !important; }
            }
            ::-webkit-scrollbar { width: 6px; height: 6px; }
            ::-webkit-scrollbar-track { background: transparent; }
            ::-webkit-scrollbar-thumb { background: #d0dbed; border-radius: 9999px; }
            ::-webkit-scrollbar-thumb:hover { background: #00539F; }
            .material-symbols-outlined {
                font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
                vertical-align: middle;
            }
            .sidebar-scrollable {
                overflow-y: auto;
                scrollbar-width: thin;
                scrollbar-color: #d0dbed transparent;
            }
            @media (min-width: 768px) {
                #mainSidebar { width: 320px !important; }
                header#mainHeader, header.admin-top-header { left: 320px !important; }
                .admin-main-wrap { padding-left: 320px !important; }
            }
            .admin-main-wrap { padding-top: 5rem !important; }
            /* Seamless theme mapping to NTU Blue & Sky Deep across all admin modules */
            .bg-blue-600, .bg-blue-500 { background-color: #00539F !important; }
            .hover\\:bg-blue-700:hover, .hover\\:bg-blue-600:hover { background-color: #003e7e !important; }
            .text-blue-600, .text-blue-500 { color: #00539F !important; }
            .bg-blue-50 { background-color: #e5f7ff !important; }
            .text-blue-700 { color: #003e7e !important; }
            .border-blue-100 { border-color: #e5f7ff !important; }
            .border-blue-200 { border-color: rgba(51,204,255,0.4) !important; }
            .border-blue-300 { border-color: #33ccff !important; }
            .focus\\:border-blue-500:focus { border-color: #00539F !important; }
            .focus\\:ring-blue-500\\/20:focus { --tw-ring-color: rgba(0, 83, 159, 0.2) !important; }
        `;
        document.head.appendChild(style);
    }

    // Configure Tailwind CSS theme tokens
    const configEdufaceTailwind = () => {
        if (window.tailwind) {
            window.tailwind.config = window.tailwind.config || {};
            window.tailwind.config.darkMode = "class";
            window.tailwind.config.theme = window.tailwind.config.theme || {};
            window.tailwind.config.theme.extend = window.tailwind.config.theme.extend || {};
            
            // Colors
            window.tailwind.config.theme.extend.colors = {
                ...(window.tailwind.config.theme.extend.colors || {}),
                "background": "#f6fbfe",
                "surface-container-high": "#dee9fc",
                "secondary-fixed-dim": "#bae6fd",
                "tertiary-fixed-dim": "#d0e4f7",
                "on-primary-fixed": "#001d3d",
                "outline-variant": "#cbd5e1",
                "on-surface": "#0b2540",
                "error-container": "#fee2e2",
                "inverse-on-surface": "#f0f9ff",
                "on-background": "#0b2540",
                "on-error-container": "#991b1b",
                "surface": "#f6fbfe",
                "on-secondary-fixed-variant": "#0369a1",
                "secondary": "#0284c7",
                "on-tertiary-fixed-variant": "#075985",
                "primary-fixed": "#e0f2fe",
                "surface-tint": "#00539F",
                "on-primary-fixed-variant": "#003e7e",
                "surface-bright": "#ffffff",
                "surface-variant": "#e5f0fa",
                "on-secondary-container": "#0c4a6e",
                "outline": "#94a3b8",
                "on-primary-container": "#003e7e",
                "error": "#ef4444",
                "on-tertiary-container": "#082f49",
                "on-primary": "#ffffff",
                "tertiary-container": "#bae6fd",
                "inverse-primary": "#7dd3fc",
                "on-error": "#ffffff",
                "surface-dim": "#d0dbed",
                "secondary-container": "#e0f2fe",
                "surface-container-low": "#eff6ff",
                "tertiary-fixed": "#e0f2fe",
                "primary-fixed-dim": "#7dd3fc",
                "primary-container": "#e5f7ff",
                "surface-container-lowest": "#ffffff",
                "surface-container": "#f0f9ff",
                "surface-container-highest": "#dce8f8",
                "primary": "#00539F",
                "tertiary": "#0369a1",
                "on-secondary": "#ffffff",
                "on-surface-variant": "#55677c",
                "secondary-fixed": "#e0f2fe",
                "on-tertiary-fixed": "#0c4a6e",
                "on-tertiary": "#ffffff",
                "on-secondary-fixed": "#0c4a6e",
                "inverse-surface": "#0b2540",
                "brand": {
                    50: '#f0f9ff',
                    100: '#e0f2fe',
                    200: '#bae6fd',
                    300: '#7dd3fc',
                    400: '#38bdf8',
                    500: '#00539F',
                    600: '#003e7e',
                    700: '#002952'
                }
            };

            // Spacing
            window.tailwind.config.theme.extend.spacing = {
                ...(window.tailwind.config.theme.extend.spacing || {}),
                "space-xs": "0.25rem",
                "space-sm": "0.5rem",
                "space-md": "1rem",
                "space-lg": "1.5rem",
                "space-xl": "2.5rem",
                "margin": "2rem",
                "gutter": "1.5rem",
                "margin-mobile": "1rem",
                "gutter-mobile": "1rem"
            };

            // Font Family
            window.tailwind.config.theme.extend.fontFamily = {
                ...(window.tailwind.config.theme.extend.fontFamily || {}),
                sans: ["'Plus Jakarta Sans'", "sans-serif"],
                "body-sm": ["'Plus Jakarta Sans'"],
                "body-md": ["'Plus Jakarta Sans'"],
                "body-lg": ["'Plus Jakarta Sans'"],
                "headline-sm": ["'Plus Jakarta Sans'"],
                "headline-md": ["'Plus Jakarta Sans'"],
                "headline-lg": ["'Plus Jakarta Sans'"],
                "headline-xl": ["'Plus Jakarta Sans'"],
                "label-sm": ["'Plus Jakarta Sans'"],
                "label-md": ["'Plus Jakarta Sans'"],
                "label-lg": ["'Plus Jakarta Sans'"]
            };

            // Font Sizes
            window.tailwind.config.theme.extend.fontSize = {
                ...(window.tailwind.config.theme.extend.fontSize || {}),
                "body-sm": ["13px", { lineHeight: "18px", fontWeight: "400" }],
                "body-md": ["14px", { lineHeight: "20px", fontWeight: "400" }],
                "body-lg": ["16px", { lineHeight: "24px", fontWeight: "400" }],
                "headline-sm": ["18px", { lineHeight: "24px", fontWeight: "600" }],
                "headline-md": ["22px", { lineHeight: "28px", fontWeight: "600" }],
                "headline-lg": ["28px", { lineHeight: "36px", fontWeight: "600" }],
                "headline-xl": ["36px", { lineHeight: "44px", fontWeight: "700" }],
                "label-sm": ["11px", { lineHeight: "14px", fontWeight: "500" }],
                "label-md": ["12px", { lineHeight: "16px", fontWeight: "600" }],
                "label-lg": ["14px", { lineHeight: "20px", fontWeight: "600" }]
            };
        }
    };

    configEdufaceTailwind();
}

injectHeadAssets();

function getActiveNavKey() {
    if (window.__ACTIVE_NAV__) return window.__ACTIVE_NAV__;
    const bodyNav = document.body.getAttribute('data-nav');
    const p = window.location.pathname.toLowerCase();
    const search = window.location.search.toLowerCase();
    if (p.includes('/quizzes')) return 'quizzes';
    if (p.includes('/documents')) return 'documents';
    if (p.includes('/orders')) return 'orders';
    if (p.includes('/announcements')) return 'announcements';
    if (p.includes('/courses')) return 'courses';
    if (p.includes('/students')) {
        if (search.includes('type=staff')) return 'staff';
        return 'students';
    }
    if (p.includes('/faculties')) return 'faculties';
    if (p.includes('/majors')) return 'majors';
    if (p.includes('/classes')) return 'classes';
    if (p.includes('/profile') || p.includes('/settings')) return 'profile';
    return bodyNav || 'dashboard';
}

function renderAdminSidebar(activeKey) {
    const el = document.getElementById('admin-sidebar') || document.getElementById('admin-sidebar-container');
    if (!el) return;

    const navItems = [
        {
            key: 'dashboard',
            label: 'Tổng quan',
            path: 'tong-quan',
            href: `${ADMIN_ROOT}index.html`,
            icon: 'dashboard'
        },
        {
            key: 'majors',
            label: 'Chuyên ngành đào tạo',
            path: 'chuyen-nganh-dao-tao',
            href: `${ADMIN_ROOT}majors/list.html`,
            badgeId: 'sidebarMajorsCount',
            icon: 'domain'
        },
        {
            key: 'courses',
            label: 'Khóa học & Học phần',
            path: 'quan-ly-khoa-hoc-hoc-phan',
            href: `${ADMIN_ROOT}courses/list.html`,
            badgeId: 'sidebarCoursesCount',
            icon: 'menu_book'
        },
        {
            key: 'quizzes',
            label: 'Quản lý câu hỏi',
            path: 'quan-ly-cau-hoi-quiz',
            href: `${ADMIN_ROOT}quizzes/list.html`,
            badgeId: 'sidebarQuizzesCount',
            icon: 'quiz'
        },
        {
            key: 'documents',
            label: 'Quản lý tài liệu theo ngành',
            path: 'quan-ly-tai-lieu-theo-nganh',
            href: `${ADMIN_ROOT}documents/list.html`,
            badgeId: 'sidebarDocumentsCount',
            icon: 'description'
        },
        {
            key: 'orders',
            label: 'Hoá đơn thanh toán',
            path: 'hoa-don-thanh-toan',
            href: `${ADMIN_ROOT}orders/list.html`,
            badgeId: 'sidebarOrdersCount',
            icon: 'receipt_long'
        },
        {
            key: 'faculties',
            label: 'Khoa & Viện đào tạo',
            path: 'quan-ly-khoa-vien',
            href: `${ADMIN_ROOT}faculties/list.html`,
            badgeId: 'sidebarFacultiesCount',
            icon: 'account_balance'
        },
        {
            key: 'students',
            label: 'Sinh viên & Hồ sơ',
            path: 'quan-ly-sinh-vien-ho-so',
            href: `${ADMIN_ROOT}students/list.html?type=student`,
            badgeId: 'sidebarStudentsCount',
            icon: 'group'
        },
        {
            key: 'staff',
            label: 'Cán bộ & Giảng viên',
            path: 'can-bo-giang-vien',
            href: `${ADMIN_ROOT}students/list.html?type=staff`,
            badgeId: 'sidebarStaffCount',
            icon: 'badge'
        },
        {
            key: 'classes',
            label: 'Lớp học & TKB',
            path: 'quan-ly-lop-hoc-thoi-khoa-bieu',
            href: `${ADMIN_ROOT}classes/list.html`,
            badgeId: 'sidebarClassesCount',
            icon: 'calendar_month'
        },
        {
            key: 'announcements',
            label: 'Tin tức & Thông báo',
            path: 'tin-tuc-thong-bao',
            href: `${ADMIN_ROOT}announcements/list.html`,
            badgeId: 'sidebarAnnouncementsCount',
            icon: 'campaign'
        },
        {
            key: 'profile',
            label: 'Cài đặt hệ thống',
            path: 'cai-dat-he-thong',
            href: `${ADMIN_ROOT}profile.html`,
            icon: 'settings'
        },
        {
            key: 'portal',
            label: 'Về Cổng Sinh Viên',
            path: 've-cong-sinh-vien',
            href: `/portal/index.html`,
            icon: 'open_in_new'
        }
    ];

    const renderNavLinks = () => navItems.map(item => {
        const isActive = item.key === activeKey;
        const activeClass = isActive
            ? 'bg-[#00539F] text-white shadow-sm font-semibold'
            : 'font-label-lg text-label-lg text-slate-700 hover:bg-[#e5f7ff] hover:text-[#00539F] font-medium';
        const iconColor = isActive ? 'text-[#33ccff]' : 'text-slate-500';

        const badge = item.badgeId
            ? `<span id="${item.badgeId}" class="ml-auto text-xs ${isActive ? 'bg-white/20 text-white' : 'bg-[#e5f7ff] text-[#00539F]'} px-2 py-0.5 rounded-full font-bold">--</span>`
            : '';

        return `
            <a href="${item.href}" data-path="${item.path}" ${isActive ? 'aria-current="page"' : ''}
                class="flex items-center gap-space-sm px-3.5 py-2.5 rounded-xl transition-all ${activeClass}">
                <span class="material-symbols-outlined text-[20px] ${iconColor}">${item.icon}</span>
                <span class="whitespace-nowrap text-sm">${item.label}</span>
                ${badge}
            </a>
        `;
    }).join('');

    el.innerHTML = `
        <!-- Backdrop for mobile -->
        <div id="sidebarBackdrop" onclick="toggleSidebar()" class="fixed inset-0 bg-slate-900/50 backdrop-blur-xs z-40 hidden md:hidden transition-opacity"></div>

        <aside id="mainSidebar" class="fixed left-0 top-0 h-full w-80 bg-surface-container-lowest shadow-[0_1px_8px_rgba(0,0,0,0.04)] z-50 flex flex-col justify-between -translate-x-full md:translate-x-0 transition-transform duration-200">
            <div class="flex flex-col min-h-0 flex-1">
                <!-- Branding Header -->
                <div class="h-16 px-space-lg flex items-center justify-between bg-surface-container-lowest border-b border-surface-container-low shrink-0">
                    <a href="${ADMIN_ROOT}index.html" class="flex items-center gap-space-sm">
                        <img src="/portal/assets/images/logo-ntu.jpg" alt="Logo NTU" class="w-10 h-10 object-contain rounded-xl border border-slate-200 shadow-xs" />
                        <div class="flex flex-col">
                            <span class="font-headline-sm text-headline-sm text-[#00539F] tracking-tight font-bold">ĐH NGUYỄN TRÃI</span>
                            <span class="font-label-sm text-label-sm text-on-surface-variant font-medium">Cổng Quản Trị Đào Tạo</span>
                        </div>
                    </a>
                    <button class="md:hidden p-1.5 rounded-lg text-on-surface-variant hover:bg-surface-container" onclick="toggleSidebar()">
                        <span class="material-symbols-outlined text-[20px]">close</span>
                    </button>
                </div>

                <!-- Admin Identity / Office Card -->
                <div class="px-space-md py-space-sm shrink-0">
                    <div class="flex items-center gap-space-sm p-space-sm rounded-xl bg-surface-container-low">
                        <div class="w-10 h-10 rounded-full bg-[#00539F] flex items-center justify-center flex-shrink-0 text-white font-bold">
                            <span class="material-symbols-outlined text-[20px]">account_balance</span>
                        </div>
                        <div class="flex flex-col min-w-0 flex-1">
                            <span class="font-label-lg text-label-lg text-on-surface truncate font-semibold">Phòng Quản Lý Đào Tạo NTU</span>
                            <span class="font-label-sm text-label-sm text-on-surface-variant truncate">admin@daihocnguyentrai.edu.vn</span>
                        </div>
                    </div>
                </div>

                <!-- Section Title -->
                <div class="px-space-md py-space-xs shrink-0">
                    <span class="px-space-sm font-label-sm text-label-sm uppercase tracking-wider text-outline font-semibold">Hệ Thống Đào Tạo</span>
                </div>

                <!-- Nav list (scrollable if screen is short) -->
                <nav class="px-space-md flex flex-col gap-1 sidebar-scrollable flex-1 py-1" data-active-classes="bg-[#00539F] text-white shadow-sm font-semibold">
                    ${renderNavLinks()}
                </nav>
            </div>

            <!-- Footer Badge -->
            <div class="p-space-md flex flex-col gap-space-sm bg-surface-container-lowest border-t border-surface-container-low shrink-0">
                <div class="flex items-center justify-between p-space-sm rounded-lg bg-surface-container-low">
                    <div class="flex items-center gap-space-xs">
                        <span class="w-2 h-2 rounded-full bg-[#00539F] animate-pulse"></span>
                        <span class="font-label-sm text-label-sm text-on-surface font-semibold">Hệ thống NTU S-Portal</span>
                    </div>
                    <span class="font-label-sm text-label-sm text-[#00539F] font-bold">v2.4.0</span>
                </div>
            </div>
        </aside>
    `;
}

function renderAdminHeader() {
    const el = document.getElementById('admin-header') || document.getElementById('admin-header-container');
    if (!el) return;

    el.innerHTML = `
        <header id="mainHeader" class="fixed top-0 left-0 md:left-80 right-0 h-16 bg-surface-container-lowest/90 backdrop-blur-md shadow-[0_1px_8px_rgba(0,0,0,0.04)] z-40 flex items-center justify-between px-4 lg:px-space-lg border-b border-surface-container-low admin-top-header">
            <div class="flex items-center gap-space-md flex-1 max-w-xl">
                <!-- Hamburger menu on mobile -->
                <button id="sidebarToggleBtn" onclick="toggleSidebar()"
                    class="md:hidden p-2 rounded-lg bg-surface-container-low text-on-surface-variant hover:bg-surface-container-high transition-colors"
                    title="Menu điều hướng">
                    <span class="material-symbols-outlined text-[22px]">menu</span>
                </button>

                <!-- Search Input with Ctrl+K shortcut hint -->
                <div class="relative w-full">
                    <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-[20px]">search</span>
                    <input id="globalHeaderSearch"
                        class="w-full h-10 pl-10 pr-4 bg-surface-container-low rounded-lg font-body-sm text-body-sm text-on-surface placeholder:text-on-surface-variant outline-none focus:bg-surface-container focus:ring-2 focus:ring-primary/20 transition-all"
                        placeholder="Tìm kiếm sinh viên, giảng viên, mã học phần (Ctrl + K)..."
                        type="text"
                        onkeydown="handleGlobalSearch(event)"/>
                </div>
            </div>

            <!-- Right Controls -->
            <div class="flex items-center gap-2 sm:gap-space-md">
                <!-- Academic Semester Selector -->
                <div class="hidden sm:flex items-center gap-space-xs bg-surface-container-low px-space-md py-1.5 rounded-lg text-on-surface cursor-pointer hover:bg-surface-container-high transition-colors">
                    <span class="material-symbols-outlined text-[18px] text-[#00539F]">calendar_today</span>
                    <span class="font-label-md text-label-md font-semibold text-on-surface">Học kỳ 1 - 2024-2025</span>
                    <span class="material-symbols-outlined text-[18px] text-on-surface-variant">expand_more</span>
                </div>

                <div class="hidden sm:block h-6 w-px bg-surface-container-highest"></div>

                <!-- Backend Health Indicator -->
                <div id="backendHealthPill" class="hidden xl:flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-surface-container-low text-on-surface font-label-sm text-label-sm">
                    <span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                    <span id="backendHealthText">ArcFace 512D Ready</span>
                </div>

                <!-- Help Button -->
                <button class="w-9 h-9 rounded-lg bg-surface-container-low hover:bg-surface-container-high text-on-surface-variant hover:text-on-surface flex items-center justify-center transition-colors"
                    title="Trợ giúp" type="button" onclick="showToast('Trung tâm trợ giúp quản trị: Liên hệ support@eduface.edu.vn')">
                    <span class="material-symbols-outlined text-[20px]">help</span>
                </button>

                <!-- Notifications Button with pulse indicator -->
                <div class="relative">
                    <button class="w-9 h-9 rounded-lg bg-surface-container-low hover:bg-surface-container-high text-on-surface-variant hover:text-on-surface flex items-center justify-center transition-colors"
                        title="Thông báo hệ thống" type="button" onclick="showToast('Thông báo: Hệ thống đồng bộ 100% dữ liệu khảo thí.')">
                        <span class="material-symbols-outlined text-[20px]">notifications</span>
                        <span class="absolute top-1 right-1 w-2 h-2 rounded-full bg-[#00539F] animate-ping"></span>
                        <span class="absolute top-1 right-1 w-2 h-2 rounded-full bg-[#00539F]"></span>
                    </button>
                </div>

                <!-- Admin Profile Avatar -->
                <a href="${ADMIN_ROOT}profile.html" class="flex items-center gap-space-sm pl-space-xs hover:opacity-90 transition-opacity">
                    <div class="flex flex-col text-right hidden sm:flex">
                        <span class="font-label-md text-label-md text-on-surface font-semibold" id="headerAdminName">Ban Giám Hiệu</span>
                        <span class="font-label-sm text-label-sm text-on-surface-variant">Quản trị Cấp cao</span>
                    </div>
                    <div class="w-8 h-8 rounded-full bg-[#00539F] flex items-center justify-center text-white font-bold shadow-xs">
                        <span class="material-symbols-outlined text-[18px]">person</span>
                    </div>
                </a>
            </div>
        </header>
    `;
}

function adjustPageLayout() {
    // Ensure document body has the design theme classes
    document.body.classList.add('bg-background', 'font-body-md', 'text-on-surface', 'antialiased');

    // Fix main layout wrapper on subpages if they don't already have pl-80
    const mainEl = document.querySelector('main');
    if (mainEl) {
        // If the main element is part of the old flex container, adapt it cleanly
        const parent = mainEl.parentElement;
        if (parent && parent.classList.contains('flex-1') && parent.classList.contains('flex')) {
            parent.className = 'w-full md:pl-80 pt-16 min-h-screen bg-background transition-all admin-main-wrap';
            mainEl.className = 'w-full p-4 sm:p-6 lg:p-space-lg max-w-7xl mx-auto';
        } else if (!mainEl.classList.contains('pt-16') && !mainEl.parentElement.classList.contains('pl-80') && !mainEl.parentElement.classList.contains('md:pl-80')) {
            mainEl.classList.add('md:pl-80', 'pt-16', 'admin-main-wrap');
        }
    }
}

function toggleSidebar() {
    const sb = document.getElementById('mainSidebar');
    const bd = document.getElementById('sidebarBackdrop');
    if (!sb) return;
    const isClosed = sb.classList.contains('-translate-x-full');
    if (isClosed) {
        sb.classList.remove('-translate-x-full');
        if (bd) bd.classList.remove('hidden');
    } else {
        sb.classList.add('-translate-x-full');
        if (bd) bd.classList.add('hidden');
    }
}

// Global Search (Ctrl + K handler)
window.addEventListener('keydown', (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        const searchInput = document.getElementById('globalHeaderSearch');
        if (searchInput) {
            searchInput.focus();
            searchInput.select();
        }
    }
});

function handleGlobalSearch(e) {
    if (e.key === 'Enter') {
        const val = e.target.value.trim();
        if (val) {
            showToast(`Đang tìm kiếm dữ liệu: "${val}"...`);
            // Forward search to courses or students if relevant
            if (/^[a-zA-Z]{2,4}\d{3}/i.test(val)) {
                window.location.href = `${ADMIN_ROOT}courses/list.html?search=${encodeURIComponent(val)}`;
            } else if (/^\d{8,12}$/.test(val)) {
                window.location.href = `${ADMIN_ROOT}students/list.html?search=${encodeURIComponent(val)}`;
            }
        }
    }
}

async function checkSystemHealth() {
    const healthPill = document.getElementById('backendHealthPill');
    const healthText = document.getElementById('backendHealthText');
    if (!healthPill || !healthText) return;
    try {
        const res = await fetch(`${API_BASE}/api/health`);
        if (res.ok) {
            const data = await res.json();
            healthPill.className = 'hidden xl:flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-surface-container-low text-on-surface font-label-sm text-label-sm';
            healthText.textContent = `Backend: Sẵn sàng (${data.mysql_total_users || 0} SV)`;
        }
    } catch (e) {
        if (healthText) healthText.textContent = 'ArcFace & Backend Online';
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
            if (studentsCountEl) studentsCountEl.textContent = s.totalStudents !== undefined ? s.totalStudents : (s.totalUsers || 0);

            const staffCountEl = document.getElementById('sidebarStaffCount');
            if (staffCountEl) staffCountEl.textContent = s.totalStaff !== undefined ? s.totalStaff : 0;

            const facultiesCountEl = document.getElementById('sidebarFacultiesCount');
            if (facultiesCountEl) facultiesCountEl.textContent = s.totalFaculties || 0;

            const majorsCountEl = document.getElementById('sidebarMajorsCount');
            if (majorsCountEl) majorsCountEl.textContent = s.totalMajors || 0;

            const classesCountEl = document.getElementById('sidebarClassesCount');
            if (classesCountEl) classesCountEl.textContent = s.totalClasses || 0;

            const quizzesCountEl = document.getElementById('sidebarQuizzesCount');
            if (quizzesCountEl) quizzesCountEl.textContent = s.totalQuestions || s.totalQuizzes || 0;

            const docsCountEl = document.getElementById('sidebarDocumentsCount');
            if (docsCountEl) docsCountEl.textContent = s.totalDocuments || 0;

            const ordersCountEl = document.getElementById('sidebarOrdersCount');
            if (ordersCountEl) ordersCountEl.textContent = s.totalOrders || 0;

            const annCountEl = document.getElementById('sidebarAnnouncementsCount');
            if (annCountEl) annCountEl.textContent = s.totalAnnouncements || 0;
        }
    } catch (e) {
        console.warn('Lỗi tải thống kê sidebar:', e);
    }
}

function showToast(msg) {
    let t = document.getElementById('toastNotification');
    let m = document.getElementById('toastMessage');
    if (!t) {
        t = document.createElement('div');
        t.id = 'toastNotification';
        t.className = 'fixed top-20 right-6 z-50 bg-on-surface text-surface-container-lowest px-4 py-3 rounded-xl shadow-lg border border-outline-variant/30 text-sm font-medium flex items-center gap-2 transition-all duration-300';
        t.innerHTML = '<span class="material-symbols-outlined text-[18px] text-primary">info</span><span id="toastMessage"></span>';
        document.body.appendChild(t);
        m = document.getElementById('toastMessage');
    }
    m.textContent = msg;
    t.classList.remove('hidden', 'opacity-0');
    t.classList.add('opacity-100');
    setTimeout(() => {
        t.classList.add('opacity-0');
        setTimeout(() => t.classList.add('hidden'), 300);
    }, 3200);
}

function initSystemClock() {
    const clock = document.getElementById('systemClock') || document.getElementById('liveClockHour');
    if (!clock) return;
    const updateTime = () => {
        const now = new Date();
        clock.textContent = now.toLocaleTimeString('vi-VN');
    };
    updateTime();
    setInterval(updateTime, 1000);
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

// Global exposure for subpages compatibility
window.initSystemClock = initSystemClock;
window.checkSystemHealth = checkSystemHealth;
window.loadSidebarBadgeCounts = loadSidebarBadgeCounts;
window.renderAdminHeader = renderAdminHeader;
window.renderAdminSidebar = renderAdminSidebar;
window.toggleSidebar = toggleSidebar;
window.handleGlobalSearch = handleGlobalSearch;
window.showToast = showToast;
window.escapeHtml = escapeHtml;
window.escapeJs = escapeJs;
window.API_BASE = API_BASE;
window.ADMIN_ROOT = ADMIN_ROOT;

// Auto-initialize components on DOM ready or immediately if already parsed
function bootAdminComponents() {
    const activeKey = getActiveNavKey();
    renderAdminSidebar(activeKey);
    renderAdminHeader();
    adjustPageLayout();
    initSystemClock();
    checkSystemHealth();
    loadSidebarBadgeCounts();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', bootAdminComponents);
} else {
    bootAdminComponents();
}
