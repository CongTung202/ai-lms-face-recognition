// =============================================================================
// LMS ENTERPRISE SHARED DATA STORE & TAILWIND V3 RESPONSIVE LAYOUT ENGINE
// =============================================================================

const DEFAULT_STORE_DATA = {
    users: [
        { id: 1, username: "admin", fullName: "Nguyễn Văn Admin", email: "admin@datn.edu.vn", role: "ADMIN", studentCode: "-", classCode: "-", faceEnrolled: false, status: "ACTIVE", avatarUrl: "", createdAt: "2026-10-01 08:00" },
        { id: 2, username: "gv_tung", fullName: "ThS. Trần Công Tùng", email: "tung.tc@datn.edu.vn", role: "INSTRUCTOR", studentCode: "-", classCode: "-", faceEnrolled: true, status: "ACTIVE", avatarUrl: "https://res.cloudinary.com/dhefmthim/image/upload/v1728800000/lms_datn/faces/usr-staff-1.jpg", createdAt: "2026-10-02 09:30" },
        { id: 3, username: "sv_an", fullName: "Lê Thị Hoàng An", email: "an.lth@student.edu.vn", role: "STUDENT", studentCode: "SV2026001", classCode: "K23CNT1", faceEnrolled: true, status: "ACTIVE", avatarUrl: "https://res.cloudinary.com/dhefmthim/image/upload/v1728800000/lms_datn/faces/EMP05_straight.jpg", createdAt: "2026-10-05 10:15" },
        { id: 4, username: "sv_binh", fullName: "Phạm Quốc Bình", email: "binh.pq@student.edu.vn", role: "STUDENT", studentCode: "SV2026002", classCode: "K23CNT1", faceEnrolled: false, status: "ACTIVE", avatarUrl: "", createdAt: "2026-10-06 11:20" },
        { id: 5, username: "sv_cuong", fullName: "Hoàng Văn Cường", email: "cuong.hv@student.edu.vn", role: "STUDENT", studentCode: "SV2026003", classCode: "K23CNT2", faceEnrolled: true, status: "LOCKED", avatarUrl: "", createdAt: "2026-10-07 14:00" }
    ],
    courses: [
        { id: 1, code: "CRS001", title: "Lập trình Java Nâng cao & Spring Boot 3", subject: "Công nghệ Phần mềm", lecturer: "ThS. Trần Công Tùng", price: 450000, chaptersCount: 4, lessonsCount: 12, status: "PUBLISHED", description: "Khóa học chuyên sâu về xây dựng RESTful APIs, bảo mật Spring Security 6, JWT và tối ưu hóa truy vấn CSDL 3NF." },
        { id: 2, code: "CRS002", title: "Trí tuệ Nhân tạo & Thị giác Máy tính (Face AI)", subject: "Trí tuệ Nhân tạo", lecturer: "ThS. Trần Công Tùng", price: 650000, chaptersCount: 3, lessonsCount: 8, status: "PUBLISHED", description: "Tìm hiểu kiến trúc OpenCV YuNet, trích xuất đặc trưng ArcFace 512D và ước tính góc quay 3D Head Pose." },
        { id: 3, code: "CRS003", title: "Cơ sở Dữ liệu Quan hệ & NoSQL MongoDB", subject: "Hệ thống Thông tin", lecturer: "Nguyễn Văn Admin", price: 350000, chaptersCount: 2, lessonsCount: 5, status: "DRAFT", description: "Thiết kế CSDL chuẩn hóa 3NF trong MySQL kết hợp lưu trữ phi cấu trúc RAG documents trong MongoDB." }
    ],
    videos: [
        { id: 1, fileName: "lesson1_intro_springboot3.mp4", lesson: "Bài 1: Tổng quan Spring Boot 3 & Maven", duration: 1840, size: "84 MB", status: "COMPRESSED", cdnUrl: "https://res.cloudinary.com/dhefmthim/video/upload/lms_datn/lesson1.mp4", resolution: "1280x720 (720p HD)" },
        { id: 2, fileName: "lesson2_jpa_mysql_3nf.mp4", lesson: "Bài 2: Thiết kế JPA Hibernate & Chuẩn 3NF", duration: 2420, size: "92 MB", status: "UPLOADED_CLOUDINARY", cdnUrl: "https://res.cloudinary.com/dhefmthim/video/upload/lms_datn/lesson2.mp4", resolution: "1280x720 (720p HD)" },
        { id: 3, fileName: "lesson3_video_streaming_206.mp4", lesson: "Bài 3: Giao thức truyền tải 206 Partial Content", duration: 3100, size: "128 MB", status: "RAW", cdnUrl: "Chưa đồng bộ CDN", resolution: "1920x1080 (Gốc)" }
    ],
    documents: [
        { id: 1, fileName: "Giao_trinh_Spring_Boot_3_Enterprise.pdf", course: "Lập trình Java Nâng cao & Spring Boot 3", format: "PDF", pages: 185, chunks: 420, status: "SYNCED", description: "Giáo trình toàn diện về cấu trúc Maven, Dependency Injection, HikariCP và Spring Data JPA." },
        { id: 2, fileName: "Bai_giang_Deep_Learning_ArcFace.docx", course: "Trí tuệ Nhân tạo & Thị giác Máy tính (Face AI)", format: "DOCX", pages: 94, chunks: 215, status: "SYNCED", description: "Bài giảng lý thuyết hàm mất mát ArcFace Cosine Margin và mô hình ResNet100." },
        { id: 3, fileName: "Huong_dan_Thuc_hanh_MySQL_3NF.pdf", course: "Cơ sở Dữ liệu Quan hệ & NoSQL MongoDB", format: "PDF", pages: 52, chunks: 110, status: "PENDING", description: "Tài liệu bài tập chuẩn hóa bảng dữ liệu và lập chỉ mục khóa ngoại." }
    ],
    exams: [
        { id: 1, code: "EX001", title: "Thi Giữa Kỳ - Kiến trúc Spring Boot 3", course: "Lập trình Java Nâng cao & Spring Boot 3", duration: 45, requireFace: true, questionsCount: 30, status: "OPEN" },
        { id: 2, code: "EX002", title: "Kiểm tra Thực hành Nhận diện AI ArcFace", course: "Trí tuệ Nhân tạo & Thị giác Máy tính (Face AI)", duration: 60, requireFace: true, questionsCount: 40, status: "OPEN" }
    ],
    violations: [
        { id: 1, studentCode: "SV2026001", studentName: "Lê Thị Hoàng An", exam: "EX001 - Kiến trúc Spring Boot 3", behavior: "Quay đầu rời mắt khỏi màn hình máy tính", details: "Góc quay Yaw +38.5 độ duy trì trong 14 giây liên tục", time: "14:22:10 15/10/2026", level: "CẢNH BÁO" },
        { id: 2, studentCode: "SV2026002", studentName: "Phạm Quốc Bình", exam: "EX001 - Kiến trúc Spring Boot 3", behavior: "Phát hiện nhiều khuôn mặt trong khung hình", details: "Mô hình YuNet phát hiện 2 khuôn mặt đồng thời", time: "14:35:44 15/10/2026", level: "ĐÌNH CHỈ" }
    ],
    orders: [
        { id: 1001, studentName: "Lê Thị Hoàng An", course: "Lập trình Java Nâng cao & Spring Boot 3", amount: 450000, status: "PAID", vietQrTx: "VCB-99238472", createdAt: "10/10/2026 09:15" },
        { id: 1002, studentName: "Phạm Quốc Bình", course: "Trí tuệ Nhân tạo & Thị giác Máy tính (Face AI)", amount: 650000, status: "PENDING", vietQrTx: "VCB-99238499", createdAt: "14/10/2026 14:20" },
        { id: 1003, studentName: "Hoàng Văn Cường", course: "Lập trình Java Nâng cao & Spring Boot 3", amount: 450000, status: "CANCELLED", vietQrTx: "VCB-99238510", createdAt: "12/10/2026 16:40" }
    ]
};

// LocalStorage Controller
const Store = {
    get() {
        const stored = localStorage.getItem("lms_admin_portal_db");
        if (stored) {
            try {
                return JSON.parse(stored);
            } catch (e) {
                console.error("Lỗi đọc dữ liệu localStorage:", e);
            }
        }
        localStorage.setItem("lms_admin_portal_db", JSON.stringify(DEFAULT_STORE_DATA));
        return JSON.parse(JSON.stringify(DEFAULT_STORE_DATA));
    },

    save(data) {
        localStorage.setItem("lms_admin_portal_db", JSON.stringify(data));
    },

    reset() {
        localStorage.setItem("lms_admin_portal_db", JSON.stringify(DEFAULT_STORE_DATA));
        return JSON.parse(JSON.stringify(DEFAULT_STORE_DATA));
    }
};

// URL Query Helpers
function getQueryParam(param) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(param);
}

// Toast notification helper
function showToast(message) {
    let toast = document.getElementById("toastNotification");
    if (!toast) {
        toast = document.createElement("div");
        toast.id = "toastNotification";
        toast.className = "toast-notification";
        document.body.appendChild(toast);
    }
    toast.innerText = message;
    toast.style.display = "block";
    setTimeout(() => {
        toast.style.display = "none";
    }, 3500);
}

// HTML escape helper
function escapeHtml(str) {
    if (!str) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

// Shared Master Layout Renderer with Tailwind CSS v3 Responsive Classes
function renderMasterLayout(activeNavKey, breadcrumbs = []) {
    const rootRelative = activeNavKey === 'dashboard' ? './' : '../';

    // Ensure Tailwind CSS v3 script is present
    if (!window.tailwind) {
        const twScript = document.createElement("script");
        twScript.src = "https://cdn.tailwindcss.com";
        document.head.appendChild(twScript);
    }

    // Top Header (Responsive flex column on mobile, row on tablet/desktop)
    const headerHtml = `
    <header class="bg-slate-950 border-b border-slate-800/80 px-4 sm:px-7 py-3.5 sticky top-0 z-50 shadow-md">
        <div class="max-w-[1440px] mx-auto flex flex-col md:flex-row md:items-center justify-between gap-3">
            <div class="flex flex-col">
                <a href="${rootRelative}index.html" class="text-sm sm:text-base font-bold text-slate-100 tracking-wider uppercase hover:text-blue-400 transition-colors">
                    LMS Core Administration &amp; AI Testing Portal
                </a>
                <span class="text-xs text-slate-400 mt-0.5">
                    Hệ Thống Quản Trị Phân Hệ &amp; Kiểm Thử Chức Năng Độc Lập
                </span>
            </div>
            <div class="flex flex-wrap items-center gap-2 sm:gap-3">
                <span id="backendStatusBadge" class="inline-flex items-center px-2.5 py-1 rounded text-xs font-semibold bg-blue-950 border border-blue-700 text-blue-200">
                    Chế độ kiểm thử cục bộ
                </span>
                <button type="button" onclick="checkBackendHealth()" class="inline-flex items-center px-3 py-1.5 rounded text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition">
                    Kiểm tra Backend
                </button>
                <a href="${rootRelative}index.html" class="inline-flex items-center px-3 py-1.5 rounded text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white transition shadow-sm">
                    Trang chủ Quản trị
                </a>
            </div>
        </div>
    </header>
    `;

    // Navigation Bar (Horizontal scroll with touch momentum on mobile)
    const navItems = [
        { key: 'dashboard', label: 'Trang tổng quan', url: `${rootRelative}index.html` },
        { key: 'students', label: 'Quản lý Sinh viên &amp; Tài khoản', url: `${rootRelative}students/list.html` },
        { key: 'courses', label: 'Quản lý Khóa học &amp; Bài học', url: `${rootRelative}courses/list.html` },
        { key: 'videos', label: 'Tài nguyên Video &amp; Nén H264', url: `${rootRelative}videos/list.html` },
        { key: 'documents', label: 'Tài liệu Giáo trình &amp; RAG AI', url: `${rootRelative}documents/list.html` },
        { key: 'exams', label: 'Đề thi &amp; Giám sát Vi phạm AI', url: `${rootRelative}exams/list.html` },
        { key: 'orders', label: 'Đơn hàng &amp; Thanh toán PayOS', url: `${rootRelative}orders/list.html` }
    ];

    let navHtml = `
    <nav class="bg-slate-900 border-b border-slate-800 px-4 sm:px-7 overflow-x-auto whitespace-nowrap scrollbar-thin">
        <div class="max-w-[1440px] mx-auto flex items-center gap-1 sm:gap-2">`;
    navItems.forEach(item => {
        const isActive = item.key === activeNavKey;
        const activeClasses = isActive 
            ? 'text-blue-400 border-b-2 border-blue-500 bg-blue-500/10 font-bold' 
            : 'text-slate-400 border-b-2 border-transparent hover:text-slate-200 hover:border-slate-700 font-medium';
        navHtml += `<a href="${item.url}" class="inline-block py-3 px-3 sm:px-4 text-xs sm:text-sm tracking-wide transition-all ${activeClasses}">${item.label}</a>`;
    });
    navHtml += `</div></nav>`;

    // Breadcrumb Bar
    let breadcrumbHtml = `
    <div class="bg-slate-950/70 border-b border-slate-800/60 px-4 sm:px-7 py-2.5 text-xs text-slate-400 overflow-x-auto whitespace-nowrap">
        <div class="max-w-[1440px] mx-auto flex items-center gap-2">
            <a href="${rootRelative}index.html" class="text-blue-400 hover:underline">Trang chủ</a>`;
    breadcrumbs.forEach(bc => {
        breadcrumbHtml += ` <span class="text-slate-600">/</span> `;
        if (bc.url) {
            breadcrumbHtml += `<a href="${bc.url}" class="text-blue-400 hover:underline">${bc.label}</a>`;
        } else {
            breadcrumbHtml += `<span class="text-slate-300 font-semibold">${bc.label}</span>`;
        }
    });
    breadcrumbHtml += `</div></div>`;

    // Insert at top of body
    document.body.insertAdjacentHTML('afterbegin', headerHtml + navHtml + breadcrumbHtml);

    // Append Toast container if not present
    if (!document.getElementById("toastNotification")) {
        const toast = document.createElement("div");
        toast.id = "toastNotification";
        toast.className = "toast-notification";
        document.body.appendChild(toast);
    }
}

async function checkBackendHealth() {
    const badge = document.getElementById("backendStatusBadge");
    if (!badge) return;
    badge.className = "inline-flex items-center px-2.5 py-1 rounded text-xs font-semibold bg-amber-950 border border-amber-700 text-amber-200";
    badge.innerText = "Đang kiểm tra kết nối...";

    try {
        const response = await fetch("http://localhost:8080/api/health", { method: "GET" });
        if (response.ok) {
            badge.className = "inline-flex items-center px-2.5 py-1 rounded text-xs font-semibold bg-emerald-950 border border-emerald-700 text-emerald-200";
            badge.innerText = "Đã kết nối Backend (Port 8080)";
            showToast("Kết nối thành công tới dịch vụ Backend!");
        } else {
            throw new Error();
        }
    } catch (e) {
        badge.className = "inline-flex items-center px-2.5 py-1 rounded text-xs font-semibold bg-blue-950 border border-blue-700 text-blue-200";
        badge.innerText = "Chế độ kiểm thử cục bộ (Backend Offline)";
        showToast("Backend chưa khởi chạy. Hệ thống đang chạy trên cơ sở dữ liệu kiểm thử cục bộ.");
    }
}
