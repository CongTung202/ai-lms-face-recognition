/**
 * NTU Smart LMS - Modular Admin Sidebar Component
 * Tự động tạo hoặc thay thế Sidebar chuẩn NTU trên tất cả các trang Admin (List, Create, Edit, Details)
 * Hỗ trợ 100% tự động nhận diện trang active, tải số lượng badge thật từ CSDL, và responsive mobile drawer.
 */

(function () {
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

  const NAV_ITEMS = [
    { key: 'dashboard', label: 'Tổng quan', href: `${ADMIN_ROOT}index.html`, icon: 'layout-dashboard', id: 'navIndex' },
    { key: 'majors', label: 'Chuyên ngành đào tạo', href: `${ADMIN_ROOT}majors/list.html`, icon: 'layers', id: 'navMajors', countId: 'navCountMajors' },
    { key: 'courses', label: 'Khóa học & Học phần', href: `${ADMIN_ROOT}courses/list.html`, icon: 'book-open', id: 'navCourses', countId: 'navCountCourses' },
    { key: 'quizzes', label: 'Quản lý câu hỏi', href: `${ADMIN_ROOT}quizzes/list.html`, icon: 'list-checks', id: 'navQuizzes', countId: 'navCountQuizzes' },
    { key: 'documents', label: 'Quản lý tài liệu theo ngành', href: `${ADMIN_ROOT}documents/list.html`, icon: 'folder-git-2', id: 'navDocuments', countId: 'navCountDocuments' },
    { key: 'orders', label: 'Hoá đơn thanh toán', href: `${ADMIN_ROOT}orders/list.html`, icon: 'receipt', id: 'navOrders', countId: 'navCountOrders' },
    { key: 'faculties', label: 'Khoa & Viện đào tạo', href: `${ADMIN_ROOT}faculties/list.html`, icon: 'building-2', id: 'navFaculties', countId: 'navCountFaculties' },
    { key: 'students', label: 'Sinh viên & Hồ sơ', href: `${ADMIN_ROOT}students/list.html?type=student`, icon: 'users', id: 'navStudents', countId: 'navCountStudents' },
    { key: 'staff', label: 'Cán bộ & Giảng viên', href: `${ADMIN_ROOT}students/list.html?type=staff`, icon: 'badge-check', id: 'navStaff', countId: 'navCountStaff' },
    { key: 'classes', label: 'Lớp học & TKB', href: `${ADMIN_ROOT}classes/list.html`, icon: 'calendar-days', id: 'navClasses', countId: 'navCountClasses' },
    { key: 'announcements', label: 'Tin tức & Thông báo', href: `${ADMIN_ROOT}announcements/list.html`, icon: 'megaphone', id: 'navAnnouncements', countId: 'navCountAnnouncements' },
    { key: 'profile', label: 'Cài đặt hệ thống', href: `${ADMIN_ROOT}profile.html`, icon: 'settings', id: 'navProfile' },
    { key: 'portal', label: 'Về Cổng Sinh Viên', href: '/portal/index.html', icon: 'external-link', isExternal: true }
  ];

  function injectSidebarStyles() {
    if (document.getElementById('ntu-admin-sidebar-style')) return;
    const style = document.createElement('style');
    style.id = 'ntu-admin-sidebar-style';
    style.textContent = `
      .side {
        width: 300px !important;
        min-width: 300px !important;
        max-width: 300px !important;
        flex-shrink: 0 !important;
        background: rgba(255, 255, 255, 0.98) !important;
        backdrop-filter: blur(16px) !important;
        -webkit-backdrop-filter: blur(16px) !important;
        border-right: 1px solid rgba(11, 37, 64, 0.08) !important;
        padding: 24px 18px !important;
        display: flex !important;
        flex-direction: column !important;
        gap: 14px !important;
        position: fixed !important;
        top: 0 !important;
        left: 0 !important;
        bottom: 0 !important;
        height: 100vh !important;
        max-height: 100vh !important;
        overflow-y: auto !important;
        overflow-x: hidden !important;
        z-index: 50 !important;
        box-sizing: border-box !important;
        transition: transform 0.3s cubic-bezier(.2,.8,.2,1) !important;
      }
      .main {
        margin-left: 300px !important;
        width: calc(100% - 300px) !important;
        min-width: 0 !important;
        box-sizing: border-box !important;
      }
      .app, .layout {
        display: block !important;
        width: 100% !important;
        min-height: 100vh !important;
      }
      .side::-webkit-scrollbar { width: 5px; }
      .side::-webkit-scrollbar-thumb { background: #d0dbed; border-radius: 999px; }
      .side .brand {
        display: flex !important;
        align-items: center !important;
        gap: 12px !important;
        padding: 6px 10px !important;
        text-decoration: none !important;
        color: inherit !important;
      }
      .side .brand-logo-img {
        width: 44px !important;
        height: 44px !important;
        border-radius: 12px !important;
        object-fit: cover !important;
        border: 1px solid rgba(51, 204, 255, 0.4) !important;
        box-shadow: 0 4px 12px rgba(0, 83, 159, 0.2) !important;
        flex-shrink: 0 !important;
      }
      .side .brand b {
        display: block !important;
        font-family: 'Fraunces', Georgia, serif !important;
        font-size: 1.15rem !important;
        font-weight: 700 !important;
        color: #00539F !important;
        line-height: 1.15 !important;
      }
      .side .brand small {
        display: block !important;
        font-size: 0.68rem !important;
        text-transform: uppercase !important;
        letter-spacing: 0.08em !important;
        color: #55677c !important;
        font-weight: 600 !important;
        margin-top: 2px !important;
      }
      .side .office {
        padding: 12px 14px !important;
        background: #e5f7ff !important;
        border: 1px solid rgba(51, 204, 255, 0.35) !important;
        border-radius: 14px !important;
        display: flex !important;
        align-items: center !important;
        gap: 10px !important;
      }
      .side .office-avatar {
        width: 36px !important;
        height: 36px !important;
        border-radius: 10px !important;
        background: #00539F !important;
        color: #ffffff !important;
        display: grid !important;
        place-items: center !important;
        flex-shrink: 0 !important;
      }
      .side .office-avatar svg { width: 18px !important; height: 18px !important; stroke-width: 2 !important; color: #fff !important; }
      .side .office-title {
        font-size: 0.8rem !important;
        color: #00539F !important;
        font-weight: 700 !important;
        line-height: 1.2 !important;
      }
      .side .office-sub {
        font-size: 0.68rem !important;
        color: #55677c !important;
        margin-top: 1px !important;
      }
      .side .label {
        font-size: 0.68rem !important;
        text-transform: uppercase !important;
        letter-spacing: 0.08em !important;
        color: #55677c !important;
        font-weight: 700 !important;
        padding: 0 10px !important;
      }
      .side nav {
        display: flex !important;
        flex-direction: column !important;
        gap: 4px !important;
      }
      .side nav a.nav-item {
        display: flex !important;
        align-items: center !important;
        gap: 12px !important;
        padding: 11px 14px !important;
        border-radius: 14px !important;
        font-size: 0.88rem !important;
        font-weight: 600 !important;
        color: #55677c !important;
        text-decoration: none !important;
        transition: background 0.25s, transform 0.25s, color 0.25s !important;
      }
      .side nav a.nav-item svg {
        width: 19px !important;
        height: 19px !important;
        color: #55677c !important;
        transition: color 0.25s !important;
        flex-shrink: 0 !important;
      }
      .side nav a.nav-item:hover {
        background: #e5f7ff !important;
        color: #00539F !important;
        transform: translateX(4px) !important;
      }
      .side nav a.nav-item:hover svg {
        color: #00539F !important;
      }
      .side nav a.nav-item.active {
        background: #0b2540 !important;
        color: #ffffff !important;
        box-shadow: 0 12px 24px -12px rgba(11, 37, 64, 0.6) !important;
        transform: none !important;
      }
      .side nav a.nav-item.active svg {
        color: #33ccff !important;
      }
      .side nav a.nav-item .count {
        margin-left: auto !important;
        min-width: 24px !important;
        height: 24px !important;
        border-radius: 12px !important;
        background: #e5f7ff !important;
        font-size: 0.72rem !important;
        display: inline-flex !important;
        align-items: center !important;
        justify-content: center !important;
        font-weight: 700 !important;
        color: #00539F !important;
        padding: 0 6px !important;
      }
      .side nav a.nav-item.active .count {
        background: #33ccff !important;
        color: #0b2540 !important;
        font-weight: 800 !important;
      }
      .side .side-foot {
        margin-top: auto !important;
        padding-top: 14px !important;
        border-top: 1px dashed rgba(11, 37, 64, 0.1) !important;
        font-size: 0.75rem !important;
        color: #55677c !important;
        display: flex !important;
        align-items: center !important;
        justify-content: space-between !important;
      }
      .side .side-foot .dot {
        width: 8px !important;
        height: 8px !important;
        border-radius: 50% !important;
        background: #059669 !important;
        display: inline-block !important;
        margin-right: 6px !important;
      }
      @media (max-width: 1024px) {
        .side {
          position: fixed !important;
          left: 0 !important;
          top: 0 !important;
          bottom: 0 !important;
          transform: translateX(-100%) !important;
          z-index: 100 !important;
          box-shadow: 0 20px 50px rgba(0,0,0,0.2) !important;
        }
        .side.open {
          transform: translateX(0) !important;
        }
        .main {
          margin-left: 0 !important;
          width: 100% !important;
        }
      }
      .sidebar-backdrop {
        position: fixed;
        inset: 0;
        background: rgba(11, 37, 64, 0.45);
        backdrop-filter: blur(4px);
        z-index: 99;
        display: none;
      }
      .sidebar-backdrop.open {
        display: block;
      }
    `;
    document.head.appendChild(style);
  }

  function detectActiveKey(container) {
    if (container && container.getAttribute('data-active')) {
      return container.getAttribute('data-active').toLowerCase();
    }
    if (document.body && document.body.getAttribute('data-nav')) {
      return document.body.getAttribute('data-nav').toLowerCase();
    }
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
    return 'dashboard';
  }

  function generateSidebarHtml(activeKey) {
    const navLinksHtml = NAV_ITEMS.map(item => {
      const isActive = item.key === activeKey;
      const activeClass = isActive ? 'active' : '';
      const countBadge = item.countId
        ? `<span class="count" id="${item.countId}">--</span>`
        : '';
      const borderTop = item.isExternal
        ? 'style="margin-top: 6px; border-top: 1px dashed rgba(11,37,64,0.1);"'
        : '';

      return `
        <a href="${item.href}" class="nav-item ${activeClass}" id="${item.id || ''}" ${borderTop}>
          <i data-lucide="${item.icon}"></i>
          <span>${item.label}</span>
          ${countBadge}
        </a>
      `;
    }).join('');

    return `
      <div class="brand">
        <img src="/portal/assets/images/logo-ntu.jpg" alt="Logo NTU" class="brand-logo-img" />
        <div>
          <b>ĐH NGUYỄN TRÃI</b>
          <small>Smart LMS · Admin Portal</small>
        </div>
      </div>

      <div class="office">
        <div class="office-avatar">
          <i data-lucide="shield-check"></i>
        </div>
        <div>
          <div class="office-title">Phòng Quản Lý Đào Tạo</div>
          <div class="office-sub">admin@daihocnguyentrai.edu.vn</div>
        </div>
      </div>

      <div class="label">HỆ THỐNG ĐÀO TẠO</div>
      <nav>
        ${navLinksHtml}
      </nav>

      <div class="side-foot">
        <span><i class="dot"></i>Hệ thống NTU S-Portal</span>
        <b>v2.4.0</b>
      </div>
    `;
  }

  async function loadSidebarStats() {
    try {
      const res = await fetch(`${API_BASE}/api/admin/stats`);
      if (!res.ok) return;
      const json = await res.json();
      const s = json.data || {};

      const mapCount = {
        navCountDashboard: s.totalCourses ? `${s.totalCourses + (s.totalStudents || 0)}` : '--',
        navCountMajors: s.totalMajors,
        navCountCourses: s.totalCourses,
        navCountQuizzes: s.totalQuestions || s.totalQuizzes,
        navCountDocuments: s.totalDocuments,
        navCountOrders: s.totalOrders,
        navCountFaculties: s.totalFaculties,
        navCountStudents: s.totalStudents !== undefined ? s.totalStudents : s.totalUsers,
        navCountStaff: s.totalStaff,
        navCountClasses: s.totalClasses,
        navCountAnnouncements: s.totalAnnouncements
      };

      for (const [id, val] of Object.entries(mapCount)) {
        if (val !== undefined && val !== null) {
          const el = document.getElementById(id);
          if (el) el.textContent = val;
        }
      }
    } catch (e) {
      console.warn('[AdminSidebar] Lỗi tải số liệu thống kê:', e);
    }
  }

  function initMobileDrawer(sideEl) {
    let backdrop = document.getElementById('sidebarBackdrop');
    if (!backdrop) {
      backdrop = document.createElement('div');
      backdrop.id = 'sidebarBackdrop';
      backdrop.className = 'sidebar-backdrop';
      document.body.appendChild(backdrop);
    }

    const toggle = () => {
      const isOpen = sideEl.classList.toggle('open');
      backdrop.classList.toggle('open', isOpen);
    };

    backdrop.onclick = () => {
      sideEl.classList.remove('open');
      backdrop.classList.remove('open');
    };

    const menuBtn = document.getElementById('menuBtn') || document.querySelector('.menu-btn');
    if (menuBtn) {
      menuBtn.onclick = (e) => {
        e.preventDefault();
        toggle();
      };
    }

    window.toggleSidebar = toggle;
  }

  function renderSidebar() {
    injectSidebarStyles();

    // Tìm thẻ chứa Sidebar
    let container = document.getElementById('admin-sidebar') ||
                    document.getElementById('side') ||
                    document.querySelector('aside.side');

    if (!container) {
      // Nếu trang chưa có, tìm layout hoặc chèn vào đầu app/body
      const layout = document.querySelector('.layout') || document.querySelector('.app');
      container = document.createElement('aside');
      container.className = 'side';
      container.id = 'side';
      if (layout) {
        layout.insertBefore(container, layout.firstChild);
      } else {
        document.body.insertBefore(container, document.body.firstChild);
      }
    }

    const activeKey = detectActiveKey(container);
    container.innerHTML = generateSidebarHtml(activeKey);
    container.setAttribute('aria-label', 'Menu quản trị');

    // Kích hoạt Lucide Icons
    if (window.lucide && typeof window.lucide.createIcons === 'function') {
      window.lucide.createIcons();
    } else {
      // Nạp Lucide nếu chưa có
      const script = document.createElement('script');
      script.src = 'https://unpkg.com/lucide@0.460.0/dist/umd/lucide.min.js';
      script.onload = () => {
        if (window.lucide) window.lucide.createIcons();
      };
      document.head.appendChild(script);
    }

    // Tải số liệu thống kê badge
    loadSidebarStats();

    // Hỗ trợ mobile drawer
    initMobileDrawer(container);
  }

  // Khởi chạy khi DOM sẵn sàng
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', renderSidebar);
  } else {
    renderSidebar();
  }

  // Xuất API toàn cục để có thể gọi lại khi cần
  window.AdminSidebar = {
    render: renderSidebar,
    refreshStats: loadSidebarStats,
    NAV_ITEMS: NAV_ITEMS
  };
})();
