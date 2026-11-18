/**
 * NTU Smart LMS - Modular Student Sidebar Component
 * Tự động tạo và chuẩn hóa Sidebar cố định (fixed) tách riêng biệt trên toàn bộ Cổng Sinh Viên (Portal)
 * Tích hợp: Menu Lịch sử đơn hàng đã mua (PayOS), Badge số lượng thật từ CSDL, Modal lịch sử đơn hàng trực quan.
 */

(function () {
  const API_BASE = (window.location.protocol === 'file:' || !window.location.origin || window.location.origin === 'null')
    ? 'http://localhost:8080'
    : window.location.origin;

  function getPortalRoot() {
    if (window.location.protocol === 'file:') {
      const p = window.location.pathname.replace(/\\/g, '/');
      const idx = p.indexOf('/student_portal/');
      if (idx !== -1) return p.substring(0, idx + 16);
      const idx2 = p.indexOf('/portal/');
      if (idx2 !== -1) return p.substring(0, idx2 + 8);
    }
    return '/portal/';
  }

  const PORTAL_ROOT = getPortalRoot();

  // Danh mục menu điều hướng Cổng Sinh Viên
  const NAV_ITEMS = [
    { key: 'home', label: 'Trang chủ', href: `${PORTAL_ROOT}index.html`, icon: 'layout-dashboard', id: 'navHome' },
    { key: 'courses', label: 'Khóa học của tôi', href: `${PORTAL_ROOT}index.html#coursesSection`, icon: 'book-open', id: 'navCourses', countId: 'navCoursesCount' },
    { key: 'study', label: 'Phòng học trực tuyến', href: `${PORTAL_ROOT}study.html`, icon: 'video', id: 'navStudy' },
    { key: 'docs', label: 'Thư viện tài liệu', href: `${PORTAL_ROOT}index.html#docsSection`, icon: 'library', id: 'navDocs', countId: 'navDocsCount' },
    { key: 'orders', label: 'Lịch sử đơn hàng', href: `${PORTAL_ROOT}orders.html`, icon: 'receipt', id: 'navOrders', countId: 'navOrdersCount' },
    { key: 'schedule', label: 'Thời khóa biểu', href: `${PORTAL_ROOT}index.html#scheduleSection`, icon: 'calendar-days', id: 'navSchedule' },
    { key: 'rag', label: 'Trợ lý AI RAG', href: `${PORTAL_ROOT}index.html#ragSection`, icon: 'sparkles', id: 'navRag' },
    { key: 'face', label: 'Đăng ký khuôn mặt AI', href: `${PORTAL_ROOT}face_enrollment.html`, icon: 'scan-face', id: 'navFace' },
    { key: 'admin', label: 'Quản trị viên', href: '/admin/index.html', icon: 'shield-check', id: 'navAdmin', isExternal: true }
  ];

  // SVG Icons Lucide dự phòng nếu Lucide icon chưa nạp
  const ICONS = {
    'layout-dashboard': '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="7" height="9" x="3" y="3" rx="1"/><rect width="7" height="5" x="14" y="3" rx="1"/><rect width="7" height="9" x="14" y="12" rx="1"/><rect width="7" height="5" x="3" y="16" rx="1"/></svg>',
    'book-open': '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>',
    'video': '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m22 8-6 4 6 4V8Z"/><rect width="14" height="12" x="2" y="6" rx="2"/></svg>',
    'library': '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m16 6 4 14"/><path d="M12 6v14"/><path d="M8 8v12"/><path d="M4 4v16"/></svg>',
    'receipt': '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1-2-1Z"/><path d="M16 8h-6a2 2 0 1 0 0 4h4a2 2 0 1 1 0 4H8"/><path d="M12 17.5v.5"/><path d="M12 6v.5"/></svg>',
    'calendar-days': '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="18" x="3" y="4" rx="2"/><path d="M16 2v4"/><path d="M8 2v4"/><path d="M3 10h18"/><path d="M8 14h.01"/><path d="M12 14h.01"/><path d="M16 14h.01"/><path d="M8 18h.01"/><path d="M12 18h.01"/><path d="M16 18h.01"/></svg>',
    'sparkles': '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m12 3-1.912 5.813a2 2 0 0 1-1.275 1.275L3 12l5.813 1.912a2 2 0 0 1 1.275 1.275L12 21l1.912-5.813a2 2 0 0 1 1.275-1.275L21 12l-5.813-1.912a2 2 0 0 1-1.275-1.275L12 3Z"/></svg>',
    'scan-face': '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 7V5a2 2 0 0 1 2-2h2"/><path d="M17 3h2a2 2 0 0 1 2 2v2"/><path d="M21 17v2a2 2 0 0 1-2 2h-2"/><path d="M7 21H5a2 2 0 0 1-2-2v-2"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><path d="M9 9h.01"/><path d="M15 9h.01"/></svg>',
    'shield-check': '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z"/><path d="m9 12 2 2 4-4"/></svg>',
    'headphones': '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 14h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a9 9 0 0 1 18 0v7a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3"/></svg>'
  };

  function injectSidebarStyles() {
    if (document.getElementById('ntu-student-sidebar-style')) return;
    const style = document.createElement('style');
    style.id = 'ntu-student-sidebar-style';
    style.textContent = `
      .sidebar {
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
      .app {
        display: block !important;
        width: 100% !important;
        min-height: 100vh !important;
      }
      .sidebar::-webkit-scrollbar { width: 5px; }
      .sidebar::-webkit-scrollbar-thumb { background: #d0dbed; border-radius: 999px; }

      .sidebar .brand {
        display: flex !important;
        align-items: center !important;
        gap: 12px !important;
        padding: 6px 10px !important;
        text-decoration: none !important;
        color: inherit !important;
      }
      .sidebar .brand-logo-img {
        width: 44px !important;
        height: 44px !important;
        border-radius: 12px !important;
        object-fit: cover !important;
        border: 1px solid rgba(51, 204, 255, 0.4) !important;
        box-shadow: 0 4px 12px rgba(0, 83, 159, 0.2) !important;
        flex-shrink: 0 !important;
      }
      .sidebar .brand b {
        display: block !important;
        font-family: 'Fraunces', Georgia, serif !important;
        font-size: 1.12rem !important;
        font-weight: 700 !important;
        color: #00539F !important;
        line-height: 1.15 !important;
      }
      .sidebar .brand small {
        display: block !important;
        font-size: 0.68rem !important;
        text-transform: uppercase !important;
        letter-spacing: 0.08em !important;
        color: #55677c !important;
        font-weight: 600 !important;
        margin-top: 2px !important;
      }
      .sidebar .nav-label {
        font-size: 0.68rem !important;
        letter-spacing: 0.12em !important;
        text-transform: uppercase !important;
        color: #8c9ba5 !important;
        font-weight: 700 !important;
        padding: 6px 12px 0 12px !important;
        margin: 0 !important;
      }
      .sidebar .nav {
        display: flex !important;
        flex-direction: column !important;
        gap: 5px !important;
        flex: 1 1 auto !important;
      }
      .sidebar .nav a {
        display: flex !important;
        align-items: center !important;
        gap: 12px !important;
        padding: 10px 14px !important;
        border-radius: 12px !important;
        color: #1a202c !important;
        font-weight: 500 !important;
        font-size: 0.88rem !important;
        text-decoration: none !important;
        transition: all 0.2s ease !important;
        position: relative !important;
      }
      .sidebar .nav a:hover {
        background: #eef6fc !important;
        color: #00539F !important;
        transform: translateX(3px) !important;
      }
      .sidebar .nav a.active {
        background: #00539F !important;
        color: #ffffff !important;
        font-weight: 600 !important;
        box-shadow: 0 4px 14px rgba(0, 83, 159, 0.3) !important;
      }
      .sidebar .nav a svg, .sidebar .nav a i {
        width: 18px !important;
        height: 18px !important;
        flex-shrink: 0 !important;
      }
      .sidebar .nav a.active svg, .sidebar .nav a.active i {
        color: #ffffff !important;
        stroke: #ffffff !important;
      }
      .sidebar .nav a .count {
        margin-left: auto !important;
        background: #eef6fc !important;
        color: #00539F !important;
        font-size: 0.72rem !important;
        font-weight: 700 !important;
        padding: 2px 7px !important;
        border-radius: 999px !important;
        border: 1px solid rgba(0, 83, 159, 0.15) !important;
      }
      .sidebar .nav a.active .count {
        background: rgba(255, 255, 255, 0.25) !important;
        color: #ffffff !important;
        border-color: transparent !important;
      }
      .sidebar .support {
        margin-top: auto !important;
        padding: 12px 14px !important;
        background: #f8fafc !important;
        border: 1px solid rgba(11, 37, 64, 0.08) !important;
        border-radius: 14px !important;
        display: flex !important;
        align-items: center !important;
        justify-content: space-between !important;
        gap: 8px !important;
      }
      .sidebar .support small {
        display: block !important;
        font-size: 0.7rem !important;
        color: #64748b !important;
        font-weight: 600 !important;
      }
      .sidebar .support a {
        display: block !important;
        font-size: 0.73rem !important;
        color: #00539F !important;
        font-weight: 600 !important;
        text-decoration: none !important;
        word-break: break-all !important;
      }
      .sidebar .support .ic {
        width: 32px !important;
        height: 32px !important;
        border-radius: 8px !important;
        background: #eef6fc !important;
        color: #00539F !important;
        display: grid !important;
        place-items: center !important;
        flex-shrink: 0 !important;
      }

      /* Modal Lịch sử đơn hàng */
      #myOrdersModal {
        display: none;
        position: fixed;
        inset: 0;
        z-index: 9999;
        background: rgba(11, 37, 64, 0.55);
        backdrop-filter: blur(8px);
        align-items: center;
        justify-content: center;
        padding: 1rem;
      }
      #myOrdersModal.open {
        display: flex !important;
      }
      .orders-modal-card {
        background: #ffffff;
        border-radius: 20px;
        width: 100%;
        max-width: 860px;
        max-height: 90vh;
        display: flex;
        flex-direction: column;
        box-shadow: 0 25px 60px -15px rgba(0, 83, 159, 0.35);
        overflow: hidden;
        animation: modalZoomIn .25s ease-out;
      }
      @keyframes modalZoomIn {
        from { opacity: 0; transform: scale(0.95); }
        to { opacity: 1; transform: scale(1); }
      }

      @media (max-width: 860px) {
        .sidebar {
          transform: translateX(-105%) !important;
          box-shadow: 20px 0 40px -20px rgba(11, 37, 64, 0.3) !important;
        }
        .sidebar.open {
          transform: none !important;
        }
        .main {
          margin-left: 0 !important;
          width: 100% !important;
        }
      }
    `;
    document.head.appendChild(style);
  }

  function renderSidebar() {
    let sidebar = document.getElementById('sidebar');
    if (!sidebar) {
      sidebar = document.createElement('aside');
      sidebar.id = 'sidebar';
      sidebar.className = 'sidebar';
      const main = document.querySelector('.main');
      if (main && main.parentNode) {
        main.parentNode.insertBefore(sidebar, main);
      } else {
        document.body.prepend(sidebar);
      }
    }

    const currentPath = window.location.pathname.replace(/\\/g, '/');
    const isStudy = currentPath.endsWith('study.html');
    const isFace = currentPath.endsWith('face_enrollment.html');
    const isOrders = currentPath.endsWith('orders.html');

    const navHtml = NAV_ITEMS.map(item => {
      let activeClass = '';
      if (item.key === 'study' && isStudy) activeClass = 'active';
      else if (item.key === 'face' && isFace) activeClass = 'active';
      else if (item.key === 'orders' && isOrders) activeClass = 'active';
      else if (item.key === 'home' && !isStudy && !isFace && !isOrders) activeClass = 'active';

      const iconSvg = ICONS[item.icon] || '';
      const countBadge = item.countId ? `<span class="count" id="${item.countId}">0</span>` : '';
      const clickAttr = item.onclick ? `onclick="${item.onclick}"` : '';

      return `
        <a href="${item.href}" class="${activeClass}" id="${item.id}" ${clickAttr} title="${item.label}">
          ${iconSvg}
          <span>${item.label}</span>
          ${countBadge}
        </a>
      `;
    }).join('');

    sidebar.innerHTML = `
      <a href="${PORTAL_ROOT}index.html" class="brand">
        <img src="${PORTAL_ROOT}assets/images/logo-ntu.jpg" alt="Logo NTU" class="brand-logo-img" onerror="this.src='/portal/assets/images/logo-ntu.jpg'" />
        <div>
          <b>ĐH NGUYỄN TRÃI</b>
          <small>Smart LMS · Cổng Sinh Viên</small>
        </div>
      </a>

      <p class="nav-label">PHÂN HỆ HỌC TẬP</p>
      <nav class="nav">
        ${navHtml}
      </nav>

      <div class="support">
        <div>
          <small>Hỗ trợ kỹ thuật & Đào tạo</small>
          <a href="mailto:hotro@daihocnguyentrai.edu.vn">hotro@daihocnguyentrai.edu.vn</a>
        </div>
        <span class="ic">${ICONS['headphones']}</span>
      </div>
    `;

    // Gắn scrim cho mobile nếu chưa có
    let scrim = document.getElementById('scrim');
    if (!scrim) {
      scrim = document.createElement('div');
      scrim.id = 'scrim';
      scrim.className = 'scrim';
      sidebar.after(scrim);
    }
    scrim.onclick = () => {
      sidebar.classList.remove('open');
      scrim.classList.remove('open');
    };

    const menuBtn = document.getElementById('menuBtn');
    if (menuBtn) {
      menuBtn.onclick = () => {
        sidebar.classList.toggle('open');
        scrim.classList.toggle('open');
      };
    }
  }

  // Tải số lượng badge từ CSDL thật
  async function fetchCounts() {
    try {
      // 1. Số khóa học
      fetch(`${API_BASE}/api/admin/courses`)
        .then(r => r.json())
        .then(d => {
          const count = d.data ? d.data.length : 0;
          const el = document.getElementById('navCoursesCount');
          if (el) el.textContent = count;
        }).catch(() => {});

      // 2. Số tài liệu
      fetch(`${API_BASE}/api/admin/documents`)
        .then(r => r.json())
        .then(d => {
          const count = d.data ? d.data.length : 0;
          const el = document.getElementById('navDocsCount');
          if (el) el.textContent = count;
        }).catch(() => {});

      // 3. Số đơn hàng đã mua
      fetchOrdersCount();
    } catch (e) {
      console.warn('[StudentSidebar] Fetch counts error:', e);
    }
  }

  async function fetchOrdersCount() {
    try {
      const uStr = sessionStorage.getItem('lms_user') || localStorage.getItem('currentUser') || localStorage.getItem('student_user');
      let url = `${API_BASE}/api/payment/my-orders`;
      if (uStr) {
        try {
          const u = JSON.parse(uStr);
          if (u.studentCode) url += `?studentCode=${encodeURIComponent(u.studentCode)}`;
          else if (u.username) url += `?username=${encodeURIComponent(u.username)}`;
        } catch (ignore) {}
      }
      const res = await fetch(url);
      if (res.ok) {
        const json = await res.json();
        const orders = json.data || [];
        const el = document.getElementById('navOrdersCount');
        if (el) {
          el.textContent = orders.length;
        }
      }
    } catch (e) {
      console.warn('[StudentSidebar] fetchOrdersCount error:', e);
    }
  }

  // Inject modal Lịch sử đơn hàng nếu chưa có
  function injectOrdersModal() {
    if (document.getElementById('myOrdersModal')) return;

    const modal = document.createElement('div');
    modal.id = 'myOrdersModal';
    modal.innerHTML = `
      <div class="orders-modal-card">
        <div style="padding:1.25rem 1.5rem; border-bottom:1px solid rgba(11,37,64,.08); display:flex; justify-content:space-between; align-items:center; background:#f8fafc;">
          <div style="display:flex; align-items:center; gap:10px;">
            <div style="width:38px; height:38px; border-radius:10px; background:#eef6fc; color:#00539F; display:grid; place-items:center;">
              ${ICONS['receipt']}
            </div>
            <div>
              <h3 style="margin:0; font-family:'Fraunces', Georgia, serif; font-size:1.2rem; color:#00539F;">Lịch Sử Đơn Hàng & Học Phí</h3>
              <p style="margin:2px 0 0; font-size:.78rem; color:#64748b;">Danh sách các khóa học đã đăng ký & giao dịch cổng thanh toán PayOS</p>
            </div>
          </div>
          <button type="button" onclick="closeMyOrdersModal()" style="background:none; border:none; font-size:1.5rem; color:#64748b; cursor:pointer; line-height:1; padding:4px 8px; border-radius:8px;">&times;</button>
        </div>

        <div style="padding:1rem 1.5rem; overflow-y:auto; flex:1 1 auto; max-height:calc(85vh - 130px);">
          <div id="ordersListContainer" style="display:flex; flex-direction:column; gap:10px;">
            <div style="padding:2rem; text-align:center; color:#64748b;">
              <div class="spinner" style="width:24px; height:24px; border:2px solid #e2e8f0; border-top-color:#00539F; border-radius:50%; animation:spin .8s linear infinite; margin:0 auto 10px;"></div>
              Đang tải danh sách đơn hàng từ PayOS & CSDL...
            </div>
          </div>
        </div>

        <div style="padding:1rem 1.5rem; border-top:1px solid rgba(11,37,64,.08); display:flex; justify-content:space-between; align-items:center; background:#f8fafc; font-size:.82rem;">
          <span style="color:#64748b;">Hệ thống thanh toán trực tuyến bảo mật bởi <b>PayOS & Ngân hàng MB</b></span>
          <button type="button" onclick="closeMyOrdersModal()" style="padding:.5rem 1.2rem; border-radius:10px; background:#00539F; color:#fff; border:none; font-weight:600; cursor:pointer;">Đóng</button>
        </div>
      </div>
    `;
    document.body.appendChild(modal);

    modal.addEventListener('click', (e) => {
      if (e.target === modal) closeMyOrdersModal();
    });
  }

  // Mở modal Lịch sử đơn hàng
  window.openMyOrdersModal = async function () {
    injectOrdersModal();
    const modal = document.getElementById('myOrdersModal');
    modal.classList.add('open');

    const container = document.getElementById('ordersListContainer');
    container.innerHTML = `
      <div style="padding:2rem; text-align:center; color:#64748b;">
        <div class="spinner" style="width:24px; height:24px; border:2px solid #e2e8f0; border-top-color:#00539F; border-radius:50%; animation:spin .8s linear infinite; margin:0 auto 10px;"></div>
        Đang nạp lịch sử đơn hàng...
      </div>
    `;

    try {
      const uStr = sessionStorage.getItem('lms_user') || localStorage.getItem('currentUser') || localStorage.getItem('student_user');
      let url = `${API_BASE}/api/payment/my-orders`;
      if (uStr) {
        try {
          const u = JSON.parse(uStr);
          if (u.studentCode) url += `?studentCode=${encodeURIComponent(u.studentCode)}`;
          else if (u.username) url += `?username=${encodeURIComponent(u.username)}`;
        } catch (ignore) {}
      }

      const res = await fetch(url);
      const json = await res.json();
      const orders = json.data || [];

      // Cập nhật lại badge
      const badge = document.getElementById('navOrdersCount');
      if (badge) badge.textContent = orders.length;

      if (orders.length === 0) {
        container.innerHTML = `
          <div style="padding:3rem 1rem; text-align:center; color:#64748b; border:1px dashed #cbd5e1; border-radius:14px;">
            <div style="font-size:2rem; margin-bottom:.5rem;">📦</div>
            <b>Bạn chưa có đơn hàng nào</b>
            <p style="margin:4px 0 0; font-size:.82rem;">Hãy đăng ký khóa học trên trang chủ để bắt đầu học tập nhé!</p>
          </div>
        `;
        return;
      }

      container.innerHTML = orders.map(o => {
        const isSuccess = o.paymentStatus === 'SUCCESS';
        const statusBadge = isSuccess
          ? `<span style="background:#dcfce7; color:#15803d; padding:3px 10px; border-radius:999px; font-size:.72rem; font-weight:700;">ĐÃ THANH TOÁN THÀNH CÔNG</span>`
          : `<span style="background:#fef3c7; color:#b45309; padding:3px 10px; border-radius:999px; font-size:.72rem; font-weight:700;">CHỜ THANH TOÁN (PENDING)</span>`;

        const dateStr = o.createdAt ? new Date(o.createdAt).toLocaleString('vi-VN') : '--';
        const amountStr = Number(o.amount || 0).toLocaleString('vi-VN') + ' đ';

        return `
          <div style="background:#fff; border:1px solid #e2e8f0; border-radius:14px; padding:1.1rem; display:flex; justify-content:space-between; align-items:center; gap:14px; box-shadow:0 2px 6px rgba(0,0,0,.03);">
            <div style="display:flex; align-items:center; gap:12px; min-width:0;">
              <div style="width:48px; height:48px; border-radius:10px; background:#eef6fc; overflow:hidden; flex-shrink:0;">
                <img src="${o.courseThumb || '/portal/assets/images/bxh-qs-05-1.jpg'}" style="width:100%; height:100%; object-fit:cover;" onerror="this.src='/portal/assets/images/bxh-qs-05-1.jpg'" />
              </div>
              <div style="min-width:0;">
                <div style="display:flex; align-items:center; gap:8px; flex-wrap:wrap; margin-bottom:4px;">
                  <b style="font-size:.92rem; color:#0f172a;">${o.courseTitle || 'Khóa học ĐH Nguyễn Trãi'}</b>
                  ${statusBadge}
                </div>
                <div style="font-size:.78rem; color:#64748b; display:flex; gap:12px; flex-wrap:wrap;">
                  <span>Mã đơn: <b style="font-family:monospace; color:#00539F;">#${o.orderCode}</b></span>
                  <span>Phương thức: <b>${o.paymentMethod || 'PayOS VietQR'}</b></span>
                  <span>Ngày: ${dateStr}</span>
                </div>
              </div>
            </div>

            <div style="text-align:right; flex-shrink:0;">
              <div style="font-size:1.05rem; font-weight:700; color:#b91c1c; margin-bottom:6px;">${amountStr}</div>
              ${isSuccess
                ? `<a href="${PORTAL_ROOT}study.html?courseId=${o.courseId || ''}" class="btn-primary" style="padding:5px 12px; border-radius:8px; font-size:.76rem; font-weight:600; text-decoration:none; display:inline-block; background:#00539F; color:#fff;">Vào Học Ngay →</a>`
                : `<a href="${PORTAL_ROOT}index.html" class="btn-ghost" style="padding:4px 10px; border-radius:8px; font-size:.76rem; font-weight:600; text-decoration:none; display:inline-block; border:1px solid #00539F; color:#00539F;">Thanh Toán Lại</a>`
              }
            </div>
          </div>
        `;
      }).join('');
    } catch (err) {
      container.innerHTML = `
        <div style="padding:2rem; text-align:center; color:#e11d48;">
          Không thể tải danh sách đơn hàng. Vui lòng thử lại sau (${err.message}).
        </div>
      `;
    }
  };

  window.closeMyOrdersModal = function () {
    const modal = document.getElementById('myOrdersModal');
    if (modal) modal.classList.remove('open');
  };

  // Khởi chạy khi DOM đã sẵn sàng
  function init() {
    injectSidebarStyles();
    renderSidebar();
    injectOrdersModal();
    fetchCounts();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
