/**
 * ==============================================================================
 * FACE AI REALTIME 3D VECTOR MESH & AUTO-CAPTURE ENGINE
 * Kết nối trực tiếp Python FastAPI (/v1/face/detect-pose & ArcFace 512D)
 * Hiển thị chính xác như giao diện Python:
 * - HUD Chip Danh tính: XÁC THỰC / TƯ THẾ
 * - 3D Vector Wireframe Mesh phát sáng thời gian thực
 * - Bounding box Corner Brackets + Tia quét Biometric Laser
 * - Tự động nhận diện khuôn mặt & Tự động chụp 3 góc (Chính diện, Trái, Phải)
 * ==============================================================================
 */

const ANGLES_CONFIG = [
    {
        key: 'straight',
        label: '1. Chính Diện',
        poseReq: 'STRAIGHT',
        desc: 'Nhìn thẳng vào camera',
        instruction: 'Hãy giữ đầu thẳng, nhìn trực diện vào camera',
        tip: 'Nhìn trực diện vào ống kính, giữ khuôn mặt cân đối ở giữa khung hình.',
        color: '#06b6d4',
        targetCheck: (pose, yaw, pitch) => pose === 'STRAIGHT' || (Math.abs(yaw) <= 9 && Math.abs(pitch) <= 9)
    },
    {
        key: 'left',
        label: '2. Nghiêng Trái',
        poseReq: 'TURN_LEFT',
        desc: 'Từ từ quay mặt sang bên trái',
        instruction: 'Xoay đầu sang phía bên trái của bạn',
        tip: 'Từ từ quay đầu sang bên trái khoảng 10° - 25°, giữ mắt nhìn tự nhiên.',
        color: '#818cf8',
        targetCheck: (pose, yaw) => pose === 'TURN_LEFT' || (yaw >= 8.0 && yaw <= 42.0)
    },
    {
        key: 'right',
        label: '3. Nghiêng Phải',
        poseReq: 'TURN_RIGHT',
        desc: 'Từ từ quay mặt sang bên phải',
        instruction: 'Xoay đầu sang phía bên phải của bạn',
        tip: 'Từ từ quay đầu sang bên phải khoảng 10° - 25° và giữ yên trong 1 giây.',
        color: '#a78bfa',
        targetCheck: (pose, yaw) => pose === 'TURN_RIGHT' || (yaw <= -8.0 && yaw >= -42.0)
    }
];

class FaceAiAutoCapture {
    constructor(options = {}) {
        this.options = Object.assign({
            apiBase: window.location.origin,
            faceAiApiBase: `http://${window.location.hostname || 'localhost'}:8001`,
            mode: 'enroll', // 'enroll' hoặc 'verify'
            onComplete: null,
            onSkip: null,
            holdDurationFrames: 5 // ~0.8s giữ đúng góc
        }, options);

        this.anglesConfig = ANGLES_CONFIG;
        this.currentStepIndex = 0;
        this.capturedImages = {};
        this.isTracking = false;
        this.trackingInterval = null;
        this.mediaStream = null;
        this.isProcessingFrame = false;
        this.showMesh = true;
        this.autoScanEnabled = true;

        // Telemetry & animation state
        this.laserY = 0;
        this.laserDirection = 1;
        this.frameCount = 0;
        this.fps = 0;
        this.lastFpsTime = performance.now();
        this.holdCounter = 0;
        this.holdProgress = 0;
        this.lastCaptureTime = 0;
        this.currentStudent = null;
        this.isSubmitting = false;
        this.autoVerifyCooldown = 0;

        this.initDomModal();
    }

    initDomModal() {
        if (document.getElementById('faceAiEnrollModal')) return;

        if (!document.getElementById('faceAiAutoCaptureStyles')) {
            const style = document.createElement('style');
            style.id = 'faceAiAutoCaptureStyles';
            style.textContent = `
                #faceAiEnrollModal {
                    display: none !important;
                    position: fixed !important;
                    inset: 0 !important;
                    z-index: 99999 !important;
                    background: rgba(2, 6, 23, 0.88) !important;
                    backdrop-filter: blur(8px) !important;
                    -webkit-backdrop-filter: blur(8px) !important;
                    align-items: center !important;
                    justify-content: center !important;
                    padding: 16px !important;
                    box-sizing: border-box !important;
                    font-family: 'Plus Jakarta Sans', system-ui, -apple-system, sans-serif !important;
                }
                #faceAiEnrollModal.open {
                    display: flex !important;
                }
                #faceAiEnrollModal.hidden {
                    display: none !important;
                }
                #faceAiEnrollModal * {
                    box-sizing: border-box;
                }
                #faceAiEnrollModal .hidden {
                    display: none !important;
                }
            `;
            document.head.appendChild(style);
        }

        const modalHtml = `
        <div id="faceAiEnrollModal" style="display: none !important;" class="fixed inset-0 z-50 bg-slate-950/85 backdrop-blur-md hidden flex items-center justify-center p-3 sm:p-5 select-none animate-in fade-in duration-200">
            <div class="bg-slate-900 border border-slate-700/80 rounded-3xl shadow-2xl max-w-4xl w-full overflow-hidden flex flex-col max-h-[96vh] text-slate-100">
                
                <!-- 1. HEADER (PHONG CÁCH PYTHON FACE AI) -->
                <div class="px-6 py-3.5 border-b border-slate-800 flex items-center justify-between bg-slate-950/70">
                    <div class="flex items-center space-x-3">
                        <div class="w-10 h-10 rounded-2xl bg-gradient-to-tr from-cyan-500 to-indigo-600 flex items-center justify-center text-white shadow-lg shadow-cyan-500/20">
                            <span class="text-xl">⚡</span>
                        </div>
                        <div>
                            <div class="flex items-center space-x-2">
                                <h3 id="faceAiModalTitle" class="text-base font-bold text-white tracking-tight">Python Face AI • 3D Vector Mesh Tracking</h3>
                                <span class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                                    <span class="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse mr-1"></span>
                                    <span>ArcFace 512D Online</span>
                                </span>
                            </div>
                            <p class="text-xs text-slate-400 mt-0.5">OpenCV YuNet + ArcFace ResNet100 • Kết nối trực tiếp máy chủ AI Python</p>
                        </div>
                    </div>
                    <button onclick="window.faceAiCaptureInstance.closeModal()" class="w-9 h-9 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white flex items-center justify-center transition border border-slate-700">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                    </button>
                </div>

                <!-- 2. ANGLE STEPS BAR (DÀNH CHO ĐĂNG KÝ 3 GÓC) -->
                <div id="faceAiStepsBar" class="px-6 py-2.5 bg-slate-950/40 border-b border-slate-800/80 flex flex-wrap items-center justify-between text-xs gap-2">
                    <div class="flex items-center space-x-3">
                        <span class="text-slate-400 font-medium">Sinh viên:</span>
                        <span id="faceAiTargetName" class="font-bold text-white">--</span>
                        <span id="faceAiTargetCode" class="font-mono font-bold text-cyan-400">--</span>
                    </div>

                    <div id="faceAiStepsContainer" class="flex items-center space-x-2 overflow-x-auto py-0.5">
                        <!-- 3 nút góc sinh trắc học -->
                    </div>
                </div>

                <!-- 3. WORKSPACE -->
                <div class="p-5 flex-1 overflow-y-auto space-y-4">
                    <!-- Controls Bar -->
                    <div class="flex items-center justify-between flex-wrap gap-2 text-xs">
                        <div class="flex items-center space-x-2">
                            <button id="btnToggleMesh" onclick="window.faceAiCaptureInstance.toggleMesh()"
                                class="px-3 py-1.5 rounded-xl font-bold border transition flex items-center space-x-1.5 bg-emerald-500/20 text-emerald-400 border-emerald-500/40">
                                <span>🕸️ Lưới Vector 3D: BẬT</span>
                            </button>
                            <button id="btnToggleAutoScan" onclick="window.faceAiCaptureInstance.toggleAutoScan()"
                                class="px-3 py-1.5 rounded-xl font-bold border transition flex items-center space-x-1.5 bg-indigo-500/20 text-indigo-400 border-indigo-500/40">
                                <span>🎯 Realtime Tracking: BẬT</span>
                            </button>
                        </div>
                        <div class="flex items-center space-x-3 font-mono text-[11px] text-slate-400">
                            <span>FPS: <strong id="faceAiFps" class="text-cyan-400">0</strong></span>
                            <span>Quay ngang (Yaw): <strong id="faceAiYaw" class="text-white">0°</strong></span>
                            <span>Ngước/Cúi (Pitch): <strong id="faceAiPitch" class="text-white">0°</strong></span>
                        </div>
                    </div>

                    <div class="grid grid-cols-1 md:grid-cols-12 gap-5 items-center">
                        
                        <!-- Left: Live Camera with Realtime 3D Mesh HUD (7 cols) -->
                        <div class="md:col-span-7 flex flex-col items-center">
                            <div class="relative w-full aspect-[4/3] bg-slate-950 rounded-2xl overflow-hidden border-2 border-slate-700 shadow-2xl flex items-center justify-center">
                                <video id="faceAiVideo" playsinline autoplay muted class="w-full h-full object-cover transform -scale-x-100"></video>
                                <canvas id="faceAiOverlayCanvas" class="absolute inset-0 w-full h-full pointer-events-none"></canvas>
                                
                                <!-- HUD CHIPS ON TOP (GIỐNG 100% PYTHON MAIN.PY) -->
                                <div class="absolute top-2.5 inset-x-2.5 flex justify-between pointer-events-none z-10 gap-2">
                                    <div id="hudIdentity" style="background: rgba(15, 23, 42, 0.88); backdrop-filter: blur(8px); border: 1px solid rgba(59, 130, 246, 0.4); border-radius: 6px; padding: 4px 10px; font-size: 11px; font-family: monospace; color: #38bdf8; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 60%;">
                                        DANH TÍNH: --
                                    </div>
                                    <div id="hudPose" style="background: rgba(15, 23, 42, 0.88); backdrop-filter: blur(8px); border: 1px solid rgba(59, 130, 246, 0.4); border-radius: 6px; padding: 4px 10px; font-size: 11px; font-family: monospace; color: #38bdf8; font-weight: 700; white-space: nowrap;">
                                        TƯ THẾ: ĐANG TÌM MẶT
                                    </div>
                                </div>

                                <!-- Flash Effect upon capture -->
                                <div id="faceAiFlashEffect" class="absolute inset-0 bg-white opacity-0 pointer-events-none transition-opacity duration-150"></div>

                                <!-- Progress Bar when holding target angle -->
                                <div id="faceAiProgressBarWrapper" class="absolute top-14 left-1/2 -translate-x-1/2 w-48 h-2 bg-slate-900/90 rounded-full border border-emerald-400/50 overflow-hidden shadow-lg z-10 hidden">
                                    <div id="faceAiProgressBar" class="h-full bg-emerald-400 rounded-full transition-all duration-150 ease-out shadow-[0_0_12px_rgba(52,211,153,0.9)]" style="width: 0%"></div>
                                </div>

                                <!-- Step Success Flash Overlay -->
                                <div id="faceAiSuccessFlash" class="absolute inset-0 bg-emerald-950/40 backdrop-blur-[2px] flex flex-col items-center justify-center hidden z-20 pointer-events-none">
                                    <div class="w-14 h-14 rounded-full bg-emerald-500 text-white flex items-center justify-center shadow-xl shadow-emerald-500/60 mb-2 ring-4 ring-white/20">
                                        <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"></path></svg>
                                    </div>
                                    <span id="faceAiSuccessFlashText" class="px-4 py-1.5 rounded-full bg-slate-900/95 border border-emerald-400 text-emerald-400 font-bold text-xs shadow-xl">
                                        Đã thu thập góc thành công!
                                    </span>
                                </div>

                                <!-- Loading Camera Placeholder -->
                                <div id="faceAiVideoPlaceholder" class="absolute inset-0 bg-slate-950 flex flex-col items-center justify-center space-y-3">
                                    <svg class="w-10 h-10 text-slate-600 animate-spin" fill="none" viewBox="0 0 24 24">
                                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
                                    </svg>
                                    <p class="text-xs text-slate-400 font-medium">Đang mở Camera &amp; AI Engine Python...</p>
                                </div>
                            </div>
                        </div>

                        <!-- Right: Guidance Card & Previews (5 cols) -->
                        <div class="md:col-span-5 flex flex-col justify-between space-y-3">
                            
                            <!-- Enrollment Right View (3 Góc) -->
                            <div id="enrollmentRightCard" class="bg-slate-950/70 border border-slate-800 rounded-2xl p-4 space-y-3">
                                <div class="flex items-center justify-between">
                                    <h4 id="guideAngleTitle" class="text-sm font-black text-cyan-400 flex items-center space-x-2">
                                        <span>1. Chính Diện</span>
                                    </h4>
                                    <span id="guideAngleBadge" class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-cyan-950 text-cyan-300 border border-cyan-800">
                                        Góc 1/3
                                    </span>
                                </div>
                                <p id="guideAngleDesc" class="text-xs text-slate-400 leading-relaxed">
                                    Nhìn thẳng vào ống kính, giữ khuôn mặt cân đối ở giữa khung hình.
                                </p>
                                <div class="p-2.5 rounded-xl bg-cyan-950/40 border border-cyan-800/40 text-[11px] text-cyan-300 leading-relaxed">
                                    💡 <strong id="guideAngleTip">Chỉ dẫn:</strong> Hãy giữ đầu thẳng, nhìn trực diện vào camera.
                                </div>

                                <!-- Thumbnails Preview Grid (3 Ô Ảnh) -->
                                <div id="capturedThumbsContainer" class="grid grid-cols-3 gap-2 pt-1">
                                    <!-- 3 góc mặt -->
                                </div>
                            </div>

                            <!-- Verification Right View -->
                            <div id="verificationRightCard" class="bg-slate-950/70 border border-slate-800 rounded-2xl p-4 space-y-3 hidden">
                                <div class="flex items-center space-x-3">
                                    <div class="w-12 h-12 rounded-2xl bg-indigo-900/60 border border-indigo-700/60 flex items-center justify-center text-indigo-400 font-bold text-lg">
                                        👤
                                    </div>
                                    <div>
                                        <h4 id="verifyStudentName" class="text-sm font-bold text-white">Sinh viên</h4>
                                        <p id="verifyStudentCode" class="text-xs font-mono text-cyan-400">Mã SV: --</p>
                                    </div>
                                </div>
                                <div class="p-3 bg-emerald-950/40 border border-emerald-800/50 rounded-2xl text-xs text-emerald-300 leading-relaxed">
                                    ⚡ <strong>Tự động nhận diện sinh trắc học:</strong> Hãy nhìn thẳng vào camera. AI sẽ tự động bắt khuôn mặt và điểm danh vào học.
                                </div>
                                <div id="verifyStatusBox" class="p-2.5 bg-slate-900 border border-slate-800 rounded-xl text-center text-xs text-slate-400 font-mono">
                                    Trạng thái: Đang theo dõi camera...
                                </div>
                            </div>

                            <!-- Actions for Enroll Mode -->
                            <div id="enrollActionsGroup" class="flex flex-col space-y-2">
                                <button type="button" onclick="window.faceAiCaptureInstance.manualCaptureCurrentAngle()"
                                    class="w-full py-2.5 rounded-2xl bg-gradient-to-r from-cyan-600 to-blue-600 hover:from-cyan-500 hover:to-blue-500 text-white font-bold text-xs shadow-md transition flex items-center justify-center space-x-2">
                                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"></path></svg>
                                    <span>Chụp Thủ Công Góc Này</span>
                                </button>
                                <div class="flex items-center space-x-2">
                                    <button type="button" onclick="window.faceAiCaptureInstance.prevStep()"
                                        class="flex-1 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold border border-slate-700 transition">
                                        Góc trước
                                    </button>
                                    <button type="button" onclick="window.faceAiCaptureInstance.nextStep()"
                                        class="flex-1 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold border border-slate-700 transition">
                                        Góc kế
                                    </button>
                                    <button type="button" onclick="window.faceAiCaptureInstance.resetAll()"
                                        class="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-rose-400 text-xs font-semibold border border-slate-700 transition">
                                        Làm lại
                                    </button>
                                </div>
                            </div>

                            <!-- Actions for Verify Mode -->
                            <div id="verifyActionsGroup" class="flex flex-col space-y-2 hidden">
                                <button type="button" id="btnSubmitVerifyInstant" onclick="window.faceAiCaptureInstance.submitVerification()"
                                    class="w-full py-3 rounded-2xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-bold text-xs shadow-lg shadow-emerald-600/30 transition flex items-center justify-center space-x-2">
                                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                                    <span id="btnSubmitVerifyText">Quét &amp; Điểm Danh Vào Học</span>
                                </button>
                                <div class="flex items-center space-x-2">
                                    <button type="button" onclick="window.faceAiCaptureInstance.switchToEnrollMode()"
                                        class="flex-1 py-2 rounded-xl bg-cyan-950/70 hover:bg-cyan-900/80 text-cyan-300 text-xs font-semibold border border-cyan-800 transition">
                                        Nạp Lại 3 Góc Mặt
                                    </button>
                                    <button type="button" onclick="window.faceAiCaptureInstance.skipAndProceed()"
                                        class="flex-1 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-slate-200 text-xs font-semibold border border-slate-700 transition">
                                        Vào học ngay
                                    </button>
                                </div>
                            </div>

                        </div>

                    </div>
                </div>

                <!-- 4. FOOTER -->
                <div class="px-6 py-4 border-t border-slate-800 bg-slate-950/70 flex items-center justify-between">
                    <div class="text-xs text-slate-400 flex items-center space-x-2">
                        <span class="w-2 h-2 rounded-full bg-emerald-400"></span>
                        <span id="faceAiFooterStatus">Tự động nhận diện khi bạn giữ đúng tư thế trước camera.</span>
                    </div>

                    <div class="flex items-center space-x-2.5">
                        <button type="button" onclick="window.faceAiCaptureInstance.closeModal()"
                            class="px-4 py-2.5 rounded-xl border border-slate-700 bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold text-xs transition">
                            Đóng
                        </button>
                        <button id="btnSubmitFaceEnroll" type="button" disabled onclick="window.faceAiCaptureInstance.submitEnrollment()"
                            class="px-5 py-2.5 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 disabled:opacity-40 disabled:cursor-not-allowed text-white font-bold text-xs shadow-lg shadow-emerald-600/30 transition flex items-center space-x-2">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                            <span id="btnSubmitFaceEnrollText">Lưu Hồ Sơ Face ID</span>
                        </button>
                    </div>
                </div>

            </div>
        </div>
        `;

        document.body.insertAdjacentHTML('beforeend', modalHtml);
    }

    open(studentData = {}, mode = 'enroll') {
        this.options.mode = mode;
        this.currentStudent = studentData;
        this.currentStepIndex = 0;
        this.capturedImages = {};
        this.holdProgress = 0;
        this.holdCounter = 0;
        this.isSubmitting = false;
        this.autoVerifyCooldown = 0;

        const modal = document.getElementById('faceAiEnrollModal');
        if (modal) {
            modal.classList.remove('hidden');
            modal.classList.add('open');
            modal.style.setProperty('display', 'flex', 'important');
        }

        document.getElementById('faceAiTargetName').textContent = studentData.fullName || studentData.username || 'Sinh viên';
        document.getElementById('faceAiTargetCode').textContent = studentData.studentCode || `SV${studentData.studentId || ''}`;
        
        if (mode === 'verify') {
            document.getElementById('faceAiModalTitle').textContent = "Điểm Danh Khuôn Mặt (1:1 Face Verification)";
            document.getElementById('faceAiStepsBar').classList.add('hidden');
            document.getElementById('btnSubmitFaceEnroll').classList.add('hidden');
            document.getElementById('enrollmentRightCard').classList.add('hidden');
            document.getElementById('enrollActionsGroup').classList.add('hidden');

            document.getElementById('verificationRightCard').classList.remove('hidden');
            document.getElementById('verifyActionsGroup').classList.remove('hidden');
            document.getElementById('verifyStudentName').textContent = studentData.fullName || 'Sinh viên';
            document.getElementById('verifyStudentCode').textContent = `Mã SV: ${studentData.studentCode || '--'}`;
            document.getElementById('faceAiFooterStatus').textContent = "Tự động nhận diện khi khuôn mặt giữ chính diện 1 giây.";
        } else {
            document.getElementById('faceAiModalTitle').textContent = "Đăng Ký Khuôn Mặt 3 Góc (FaceID 3D)";
            document.getElementById('faceAiStepsBar').classList.remove('hidden');
            document.getElementById('btnSubmitFaceEnroll').classList.remove('hidden');
            document.getElementById('btnSubmitFaceEnroll').disabled = true;
            document.getElementById('enrollmentRightCard').classList.remove('hidden');
            document.getElementById('enrollActionsGroup').classList.remove('hidden');

            document.getElementById('verificationRightCard').classList.add('hidden');
            document.getElementById('verifyActionsGroup').classList.add('hidden');
            document.getElementById('faceAiFooterStatus').textContent = "Tự động chụp khi bạn giữ đúng góc mặt yêu cầu 1 giây.";

            this.renderAngleStepBadges();
            this.renderThumbnailsGrid();
            this.updateCurrentStepDisplay();
        }

        this.startCameraAndTracking();
    }

    switchToEnrollMode() {
        this.open(this.currentStudent, 'enroll');
    }

    closeModal() {
        this.stopTracking();
        const modal = document.getElementById('faceAiEnrollModal');
        if (modal) {
            modal.classList.remove('open');
            modal.classList.add('hidden');
            modal.style.setProperty('display', 'none', 'important');
        }
    }

    skipAndProceed() {
        this.closeModal();
        if (typeof this.options.onSkip === 'function') {
            this.options.onSkip();
        } else if (typeof this.options.onComplete === 'function') {
            this.options.onComplete(this.currentStudent);
        }
    }

    toggleMesh() {
        this.showMesh = !this.showMesh;
        const btn = document.getElementById('btnToggleMesh');
        if (btn) {
            btn.className = this.showMesh 
                ? "px-3 py-1.5 rounded-xl font-bold border transition flex items-center space-x-1.5 bg-emerald-500/20 text-emerald-400 border-emerald-500/40"
                : "px-3 py-1.5 rounded-xl font-bold border transition flex items-center space-x-1.5 bg-slate-800 text-slate-400 border-slate-700";
            btn.textContent = `🕸️ Lưới Vector 3D: ${this.showMesh ? 'BẬT' : 'TẮT'}`;
        }
    }

    toggleAutoScan() {
        this.autoScanEnabled = !this.autoScanEnabled;
        const btn = document.getElementById('btnToggleAutoScan');
        if (btn) {
            btn.className = this.autoScanEnabled
                ? "px-3 py-1.5 rounded-xl font-bold border transition flex items-center space-x-1.5 bg-indigo-500/20 text-indigo-400 border-indigo-500/40"
                : "px-3 py-1.5 rounded-xl font-bold border transition flex items-center space-x-1.5 bg-slate-800 text-slate-400 border-slate-700";
            btn.textContent = `🎯 Realtime Tracking: ${this.autoScanEnabled ? 'BẬT' : 'TẮT'}`;
        }
    }

    renderAngleStepBadges() {
        const container = document.getElementById('faceAiStepsContainer');
        if (!container) return;

        container.innerHTML = this.anglesConfig.map((ang, idx) => {
            const isDone = Boolean(this.capturedImages[ang.key]);
            const isActive = this.currentStepIndex === idx;

            let badgeClass = "px-3 py-1.5 rounded-xl font-bold text-xs border transition cursor-pointer flex items-center space-x-1 ";
            if (isDone) {
                badgeClass += "bg-emerald-500/20 text-emerald-300 border-emerald-500/40";
            } else if (isActive) {
                badgeClass += "bg-cyan-500/20 text-cyan-300 border-cyan-500/60 ring-1 ring-cyan-400";
            } else {
                badgeClass += "bg-slate-800/80 text-slate-400 border-slate-700 hover:bg-slate-700";
            }

            return `
            <button type="button" onclick="window.faceAiCaptureInstance.goToStep(${idx})" class="${badgeClass}">
                <span>${isDone ? '✓' : idx + 1}. ${ang.label.replace(/^\d+\.\s*/, '')}</span>
            </button>
            `;
        }).join('');
    }

    renderThumbnailsGrid() {
        const container = document.getElementById('capturedThumbsContainer');
        if (!container) return;

        container.innerHTML = this.anglesConfig.map((ang) => {
            const imgData = this.capturedImages[ang.key];
            if (imgData) {
                return `
                <div class="aspect-square rounded-xl overflow-hidden border-2 border-emerald-500 bg-slate-900 relative group shadow-md">
                    <img src="${imgData}" class="w-full h-full object-cover">
                    <div class="absolute inset-0 bg-slate-950/70 opacity-0 group-hover:opacity-100 flex items-center justify-center transition">
                        <span class="text-[10px] font-bold text-emerald-300 uppercase">${ang.key}</span>
                    </div>
                </div>
                `;
            }
            return `
            <div class="aspect-square rounded-xl border-2 border-dashed border-slate-800 bg-slate-900/60 flex flex-col items-center justify-center text-xs text-slate-500 font-bold">
                <span>${ang.key}</span>
                <span class="text-[10px] text-slate-600">Chưa chụp</span>
            </div>
            `;
        }).join('');
    }

    updateCurrentStepDisplay() {
        const cur = this.anglesConfig[this.currentStepIndex];
        if (!cur) return;

        const titleEl = document.getElementById('guideAngleTitle');
        const badgeEl = document.getElementById('guideAngleBadge');
        const descEl = document.getElementById('guideAngleDesc');
        const tipEl = document.getElementById('guideAngleTip');

        if (titleEl) titleEl.textContent = cur.label;
        if (badgeEl) badgeEl.textContent = `Góc ${this.currentStepIndex + 1}/${this.anglesConfig.length}`;
        if (descEl) descEl.textContent = cur.desc;
        if (tipEl) tipEl.textContent = cur.tip || cur.instruction;
    }

    goToStep(index) {
        if (index >= 0 && index < this.anglesConfig.length) {
            this.currentStepIndex = index;
            this.holdCounter = 0;
            this.holdProgress = 0;
            this.renderAngleStepBadges();
            this.updateCurrentStepDisplay();
        }
    }

    nextStep() {
        if (this.currentStepIndex < this.anglesConfig.length - 1) {
            this.goToStep(this.currentStepIndex + 1);
        }
    }

    prevStep() {
        if (this.currentStepIndex > 0) {
            this.goToStep(this.currentStepIndex - 1);
        }
    }

    resetAll() {
        this.capturedImages = {};
        this.currentStepIndex = 0;
        this.holdCounter = 0;
        this.holdProgress = 0;
        this.renderThumbnailsGrid();
        this.renderAngleStepBadges();
        this.updateCurrentStepDisplay();
        const btn = document.getElementById('btnSubmitFaceEnroll');
        if (btn) {
            btn.disabled = true;
            btn.classList.remove('ring-4', 'ring-emerald-400', 'animate-pulse');
        }
    }

    /**
     * MỞ CAMERA VÀ KÍCH HOẠT VÒNG LẶP TRACKING (GIỐNG HỆT MAIN.PY)
     */
    async startCameraAndTracking() {
        const placeholder = document.getElementById('faceAiVideoPlaceholder');
        const video = document.getElementById('faceAiVideo');
        const overlayCanvas = document.getElementById('faceAiOverlayCanvas');
        if (placeholder) placeholder.classList.remove('hidden');

        try {
            if (this.mediaStream) {
                this.mediaStream.getTracks().forEach(t => t.stop());
                this.mediaStream = null;
            }

            const stream = await navigator.mediaDevices.getUserMedia({
                video: { width: { ideal: 640 }, height: { ideal: 480 }, facingMode: 'user' },
                audio: false
            });

            this.mediaStream = stream;
            video.srcObject = stream;
            await video.play();

            if (placeholder) placeholder.classList.add('hidden');
            this.isTracking = true;

            const initCanvas = () => {
                if (video.videoWidth > 0 && overlayCanvas) {
                    overlayCanvas.width = video.videoWidth;
                    overlayCanvas.height = video.videoHeight;
                    this.startPythonAiTrackingLoop();
                }
            };

            video.onloadedmetadata = initCanvas;
            initCanvas();

        } catch (err) {
            console.error("Camera error:", err);
            if (placeholder) {
                placeholder.innerHTML = `
                    <div class="text-rose-400 font-bold text-xs p-4 text-center">
                        ⚠️ Không thể mở Camera: ${err.message}<br>
                        Vui lòng kiểm tra quyền truy cập webcam trên trình duyệt.
                    </div>
                `;
            }
        }
    }

    stopTracking() {
        this.isTracking = false;
        if (this.trackingInterval) {
            clearInterval(this.trackingInterval);
            this.trackingInterval = null;
        }
        if (this.mediaStream) {
            this.mediaStream.getTracks().forEach(t => t.stop());
            this.mediaStream = null;
        }
        const video = document.getElementById('faceAiVideo');
        if (video) video.srcObject = null;
    }

    /**
     * VÒNG LẶP GỌI PYTHON FASTAPI DETECT-POSE (CHU KỲ 100MS - CHÍNH XÁC NHƯ MAIN.PY)
     */
    startPythonAiTrackingLoop() {
        if (this.trackingInterval) clearInterval(this.trackingInterval);

        const video = document.getElementById('faceAiVideo');
        const overlayCanvas = document.getElementById('faceAiOverlayCanvas');
        if (!video || !overlayCanvas) return;
        const overlayCtx = overlayCanvas.getContext('2d');

        // Canvas nén xử lý ảnh
        const processCanvas = document.createElement('canvas');

        this.trackingInterval = setInterval(async () => {
            if (!this.isTracking || !video.videoWidth || this.isProcessingFrame) return;

            processCanvas.width = video.videoWidth;
            processCanvas.height = video.videoHeight;
            const ctx = processCanvas.getContext('2d');
            ctx.drawImage(video, 0, 0, processCanvas.width, processCanvas.height);

            // FPS Counter
            this.frameCount++;
            const now = performance.now();
            if (now - this.lastFpsTime >= 1000) {
                this.fps = this.frameCount;
                this.frameCount = 0;
                this.lastFpsTime = now;
                const fpsEl = document.getElementById('faceAiFps');
                if (fpsEl) fpsEl.textContent = this.fps;
            }

            this.isProcessingFrame = true;

            processCanvas.toBlob(async (blob) => {
                if (!blob) {
                    this.isProcessingFrame = false;
                    return;
                }

                const formData = new FormData();
                formData.append('image', blob, 'frame.jpg');

                try {
                    // Gọi trực tiếp Python FastAPI microservice
                    const res = await fetch(`${this.options.faceAiApiBase}/v1/face/detect-pose`, {
                        method: 'POST',
                        body: formData
                    });

                    if (res.ok) {
                        const data = await res.json();
                        this.drawHUDAndMesh(data, overlayCanvas, overlayCtx);
                        this.handleProgressionAndAutoScan(data);
                    }
                } catch (err) {
                    // Python AI đang khởi động hoặc chưa sẵn sàng
                } finally {
                    this.isProcessingFrame = false;
                }
            }, 'image/jpeg', 0.85);

        }, 110);
    }

    /**
     * VẼ HUD & LƯỚI VECTOR 3D (CHÍNH XÁC 100% NHƯ MAIN.PY)
     */
    drawHUDAndMesh(data, overlayCanvas, overlayCtx) {
        overlayCtx.clearRect(0, 0, overlayCanvas.width, overlayCanvas.height);

        const hudIdentity = document.getElementById('hudIdentity');
        const hudPose = document.getElementById('hudPose');

        if (!data || !data.face_detected || !data.bbox) {
            if (hudIdentity) hudIdentity.innerText = "DANH TÍNH: --";
            if (hudPose) hudPose.innerText = "TƯ THẾ: ĐANG TÌM MẶT";
            const pbw = document.getElementById('faceAiProgressBarWrapper');
            if (pbw) pbw.classList.add('hidden');
            return;
        }

        // Mirror X coordinates for canvas to match scaleX(-1) mirrored video
        const rawBox = data.bbox;
        const width = rawBox.width;
        const height = rawBox.height;
        const x = overlayCanvas.width - (rawBox.x + width);
        const y = rawBox.y;

        const pose = data.pose ? data.pose.pose : "STRAIGHT";
        const yawDeg = data.pose?.yaw_deg ?? 0;
        const pitchDeg = data.pose?.pitch_deg ?? 0;

        document.getElementById('faceAiYaw').textContent = `${yawDeg > 0 ? '+' : ''}${yawDeg.toFixed(1)}°`;
        document.getElementById('faceAiPitch').textContent = `${pitchDeg > 0 ? '+' : ''}${pitchDeg.toFixed(1)}°`;

        const identity = data.identity || {};
        const isMatched = identity.matched === true;
        const fullName = identity.full_name;
        const empId = identity.employee_id;
        const similarity = identity.similarity ? (identity.similarity * 100).toFixed(1) : 0;

        const poseMap = {
            "STRAIGHT": { icon: "👤", text: "NHÌN THẲNG (CHÍNH DIỆN)" },
            "TURN_LEFT": { icon: "👈", text: "QUAY TRÁI (BÊN TRÁI)" },
            "TURN_RIGHT": { icon: "👉", text: "QUAY PHẢI (BÊN PHẢI)" },
            "LOOK_UP": { icon: "👆", text: "NGƯỚC LÊN (PHÍA TRÊN)" },
            "LOOK_DOWN": { icon: "👇", text: "CÚI XUỐNG (PHÍA DƯỚI)" }
        };
        const currentPoseInfo = poseMap[pose] || { icon: "🎯", text: pose };

        if (hudPose) {
            hudPose.innerText = `TƯ THẾ: ${currentPoseInfo.icon} ${currentPoseInfo.text}`;
        }

        if (hudIdentity) {
            if (isMatched && fullName) {
                hudIdentity.innerText = `XÁC THỰC: ${fullName} (${empId}) - ${similarity}%`;
                hudIdentity.style.color = "#34d399";
            } else if (this.currentStudent) {
                hudIdentity.innerText = `DANH TÍNH: ${this.currentStudent.fullName || 'Sinh viên'}`;
                hudIdentity.style.color = "#38bdf8";
            } else {
                hudIdentity.innerText = `DANH TÍNH: CHƯA ĐĂNG KÝ / KHÁCH`;
                hudIdentity.style.color = "#fbbf24";
            }
        }

        const currentAng = this.anglesConfig[this.currentStepIndex] || this.anglesConfig[0];
        const isTargetMet = this.options.mode === 'verify'
            ? (isMatched || pose === 'STRAIGHT' || (Math.abs(yawDeg) <= 9 && Math.abs(pitchDeg) <= 9))
            : (currentAng.targetCheck(pose, yawDeg, pitchDeg));

        const mainColor = isMatched ? "#10b981" : (isTargetMet ? "#10b981" : "#06b6d4");
        const glowColor = isMatched ? "rgba(16, 185, 129, 0.45)" : (isTargetMet ? "rgba(16, 185, 129, 0.4)" : "rgba(6, 182, 212, 0.4)");

        overlayCtx.save();

        // 1. RENDER 3D VECTOR FACE MESH WIREFRAME (GIỐNG HỆT MAIN.PY)
        if (this.showMesh && data.landmarks) {
            const lm = data.landmarks;
            const mirrorX = (px) => overlayCanvas.width - px;

            const re = { x: mirrorX(lm.right_eye.x), y: lm.right_eye.y };
            const le = { x: mirrorX(lm.left_eye.x), y: lm.left_eye.y };
            const nose = { x: mirrorX(lm.nose_tip.x), y: lm.nose_tip.y };
            const mr = { x: mirrorX(lm.mouth_right.x), y: lm.mouth_right.y };
            const ml = { x: mirrorX(lm.mouth_left.x), y: lm.mouth_left.y };

            const foreheadL = { x: x + width * 0.7, y: y + height * 0.1 };
            const foreheadR = { x: x + width * 0.3, y: y + height * 0.1 };
            const foreheadMid = { x: x + width * 0.5, y: y + height * 0.05 };
            const templeL = { x: x + width * 0.92, y: y + height * 0.28 };
            const templeR = { x: x + width * 0.08, y: y + height * 0.28 };
            const cheekL = { x: x + width * 0.88, y: y + height * 0.62 };
            const cheekR = { x: x + width * 0.12, y: y + height * 0.62 };
            const jawL = { x: x + width * 0.75, y: y + height * 0.9 };
            const jawR = { x: x + width * 0.25, y: y + height * 0.9 };
            const chin = { x: x + width * 0.5, y: y + height * 0.98 };
            const noseBridge = { x: (re.x + le.x) / 2, y: (re.y + le.y) / 2 - height * 0.05 };

            const meshLines = [
                [foreheadMid, foreheadL], [foreheadMid, foreheadR],
                [foreheadL, templeL], [foreheadR, templeR],
                [templeL, cheekL], [templeR, cheekR],
                [cheekL, jawL], [cheekR, jawR],
                [jawL, chin], [jawR, chin],
                [foreheadL, le], [foreheadR, re], [foreheadMid, noseBridge],
                [templeL, le], [templeR, re],
                [re, le], [re, noseBridge], [le, noseBridge],
                [noseBridge, nose], [re, nose], [le, nose],
                [cheekL, ml], [cheekR, mr],
                [cheekL, nose], [cheekR, nose],
                [nose, mr], [nose, ml],
                [mr, ml], [mr, jawR], [ml, jawL], [mr, chin], [ml, chin]
            ];

            overlayCtx.strokeStyle = glowColor;
            overlayCtx.lineWidth = 1.3;
            overlayCtx.beginPath();
            for (const [p1, p2] of meshLines) {
                overlayCtx.moveTo(p1.x, p1.y);
                overlayCtx.lineTo(p2.x, p2.y);
            }
            overlayCtx.stroke();

            const allNodes = [foreheadMid, foreheadL, foreheadR, templeL, templeR, re, le, noseBridge, nose, cheekL, cheekR, mr, ml, jawL, jawR, chin];
            overlayCtx.fillStyle = mainColor;
            overlayCtx.shadowColor = mainColor;
            overlayCtx.shadowBlur = 8;
            for (const node of allNodes) {
                overlayCtx.beginPath();
                overlayCtx.arc(node.x, node.y, 2.5, 0, 2 * Math.PI);
                overlayCtx.fill();
            }

            // Laser scan line
            this.laserY += 3 * this.laserDirection;
            if (this.laserY > height || this.laserY < 0) this.laserDirection *= -1;
            const scanLineY = y + this.laserY;

            overlayCtx.strokeStyle = isMatched ? "rgba(16, 185, 129, 0.85)" : "rgba(6, 182, 212, 0.85)";
            overlayCtx.lineWidth = 2;
            overlayCtx.shadowColor = mainColor;
            overlayCtx.shadowBlur = 12;
            overlayCtx.beginPath();
            overlayCtx.moveTo(x, scanLineY);
            overlayCtx.lineTo(x + width, scanLineY);
            overlayCtx.stroke();
        }

        // 2. CORNER BRACKETS BOUNDING BOX
        const cornerLength = Math.min(width, height) * 0.2;
        overlayCtx.strokeStyle = mainColor;
        overlayCtx.lineWidth = 3.5;
        overlayCtx.shadowColor = mainColor;
        overlayCtx.shadowBlur = 12;

        overlayCtx.beginPath();
        overlayCtx.moveTo(x, y + cornerLength); overlayCtx.lineTo(x, y); overlayCtx.lineTo(x + cornerLength, y);
        overlayCtx.moveTo(x + width - cornerLength, y); overlayCtx.lineTo(x + width, y); overlayCtx.lineTo(x + width, y + cornerLength);
        overlayCtx.moveTo(x + width, y + height - cornerLength); overlayCtx.lineTo(x + width, y + height); overlayCtx.lineTo(x + width - cornerLength, y + height);
        overlayCtx.moveTo(x + cornerLength, y + height); overlayCtx.lineTo(x, y + height); overlayCtx.lineTo(x, y + height - cornerLength);
        overlayCtx.stroke();

        // 3. READABLE NAME BADGE ON TOP OF FACE BOX
        const badgeH = 28;
        const badgeW = Math.max(180, width * 0.95);
        const badgeX = x + (width - badgeW) / 2;
        const badgeY = y - badgeH - 12;

        overlayCtx.shadowBlur = 0;
        overlayCtx.fillStyle = isMatched ? "rgba(6, 78, 59, 0.92)" : (isTargetMet ? "rgba(6, 78, 59, 0.9)" : "rgba(15, 23, 42, 0.9)");
        overlayCtx.beginPath();
        if (overlayCtx.roundRect) {
            overlayCtx.roundRect(badgeX, badgeY, badgeW, badgeH, 6);
        } else {
            overlayCtx.rect(badgeX, badgeY, badgeW, badgeH);
        }
        overlayCtx.fill();

        overlayCtx.strokeStyle = mainColor;
        overlayCtx.lineWidth = 1.5;
        overlayCtx.stroke();

        overlayCtx.fillStyle = "#ffffff";
        overlayCtx.font = "bold 13px Outfit, sans-serif";
        overlayCtx.textAlign = "center";

        if (isMatched && fullName) {
            overlayCtx.fillText(`✅ ${fullName} (${empId}) • ${similarity}%`, badgeX + badgeW / 2, badgeY + 18);
        } else if (this.options.mode === 'verify') {
            overlayCtx.fillText(isTargetMet ? `✨ ĐÃ ĐẠT GÓC: GIỮ NGUYÊN` : `${currentPoseInfo.icon} ${currentPoseInfo.text}`, badgeX + badgeW / 2, badgeY + 18);
        } else {
            const allCompleted = this.anglesConfig.every(ang => Boolean(this.capturedImages[ang.key]));
            if (allCompleted) {
                overlayCtx.fillText(`✅ ĐÃ HOÀN TẤT 3 GÓC • BẤM LƯU HỒ SƠ`, badgeX + badgeW / 2, badgeY + 18);
            } else if (this.capturedImages[currentAng.key]) {
                overlayCtx.fillText(`✅ ĐÃ CHỤP GÓC NÀY`, badgeX + badgeW / 2, badgeY + 18);
            } else {
                overlayCtx.fillText(isTargetMet ? `✨ ĐÃ ĐẠT: ${currentAng.label}` : `${currentPoseInfo.icon} ${currentPoseInfo.text} • Cần: ${currentAng.label}`, badgeX + badgeW / 2, badgeY + 18);
            }
        }

        overlayCtx.restore();
    }

    /**
     * TỰ ĐỘNG CHỤP GÓC (ENROLL) HOẶC TỰ ĐỘNG ĐIỂM DANH (VERIFY)
     */
    handleProgressionAndAutoScan(data) {
        if (!data || !data.face_detected) return;

        const pbw = document.getElementById('faceAiProgressBarWrapper');
        const pb = document.getElementById('faceAiProgressBar');
        const currentAng = this.anglesConfig[this.currentStepIndex] || this.anglesConfig[0];
        const pose = data.pose ? data.pose.pose : 'STRAIGHT';
        const yawDeg = data.pose?.yaw_deg ?? 0;
        const pitchDeg = data.pose?.pitch_deg ?? 0;

        // CHẾ ĐỘ ĐIỂM DANH ĐĂNG NHẬP (VERIFY)
        if (this.options.mode === 'verify') {
            if (this.isSubmitting || Date.now() < this.autoVerifyCooldown) return;

            const isMatched = data.identity?.matched === true;
            const isPoseStraight = (pose === 'STRAIGHT' || (Math.abs(yawDeg) <= 9 && Math.abs(pitchDeg) <= 9));

            if (isMatched || isPoseStraight) {
                this.holdCounter++;
                this.holdProgress = Math.min(100, Math.round((this.holdCounter / this.options.holdDurationFrames) * 100));

                if (pbw) pbw.classList.remove('hidden');
                if (pb) pb.style.width = `${this.holdProgress}%`;

                if (this.autoScanEnabled && this.holdProgress >= 100) {
                    this.holdCounter = 0;
                    this.holdProgress = 0;
                    if (pbw) pbw.classList.add('hidden');
                    this.submitVerification();
                }
            } else {
                this.holdCounter = Math.max(0, this.holdCounter - 1);
                this.holdProgress = Math.min(100, Math.round((this.holdCounter / this.options.holdDurationFrames) * 100));
                if (pb) pb.style.width = `${this.holdProgress}%`;
                if (this.holdProgress === 0 && pbw) pbw.classList.add('hidden');
            }
            return;
        }

        // CHẾ ĐỘ ĐĂNG KÝ 3 GÓC (ENROLL)
        const allCompleted = this.anglesConfig.every(ang => Boolean(this.capturedImages[ang.key]));
        const isCurrentAngleAlreadyCaptured = Boolean(this.capturedImages[currentAng.key]);

        // Nếu đã hoàn thành đủ cả 3 góc hoặc góc hiện tại đã được chụp: DỪNG AUTO-SCAN, không lặp lại thông báo!
        if (allCompleted || isCurrentAngleAlreadyCaptured) {
            this.holdCounter = 0;
            this.holdProgress = 0;
            if (pbw) pbw.classList.add('hidden');
            return;
        }

        const isTargetMet = currentAng.targetCheck(pose, yawDeg, pitchDeg);

        if (isTargetMet) {
            this.holdCounter++;
            this.holdProgress = Math.min(100, Math.round((this.holdCounter / this.options.holdDurationFrames) * 100));

            if (pbw) pbw.classList.remove('hidden');
            if (pb) pb.style.width = `${this.holdProgress}%`;

            if (this.autoScanEnabled && this.holdProgress >= 100) {
                if (Date.now() - this.lastCaptureTime > 800) {
                    this.lastCaptureTime = Date.now();
                    this.holdCounter = 0;
                    this.holdProgress = 0;
                    this.captureCurrentAngle();
                }
            }
        } else {
            this.holdCounter = Math.max(0, this.holdCounter - 1);
            this.holdProgress = Math.min(100, Math.round((this.holdCounter / this.options.holdDurationFrames) * 100));
            if (pb) pb.style.width = `${this.holdProgress}%`;
            if (this.holdProgress === 0 && pbw) pbw.classList.add('hidden');
        }
    }

    captureCurrentAngle() {
        const video = document.getElementById('faceAiVideo');
        if (!video || !video.videoWidth) return;

        const snapCanvas = document.createElement('canvas');
        snapCanvas.width = video.videoWidth;
        snapCanvas.height = video.videoHeight;
        const snapCtx = snapCanvas.getContext('2d');
        snapCtx.drawImage(video, 0, 0, snapCanvas.width, snapCanvas.height);
        const dataUrl = snapCanvas.toDataURL('image/jpeg', 0.92);

        const currentAng = this.anglesConfig[this.currentStepIndex];
        this.capturedImages[currentAng.key] = dataUrl;

        // Reset progress bar ngay lập tức
        const pbw = document.getElementById('faceAiProgressBarWrapper');
        if (pbw) pbw.classList.add('hidden');
        this.holdCounter = 0;
        this.holdProgress = 0;

        // Flash Effect
        const flash = document.getElementById('faceAiFlashEffect');
        if (flash) {
            flash.style.opacity = '0.85';
            setTimeout(() => { flash.style.opacity = '0'; }, 180);
        }

        const successFlash = document.getElementById('faceAiSuccessFlash');
        const successFlashText = document.getElementById('faceAiSuccessFlashText');
        if (successFlash && successFlashText) {
            successFlashText.textContent = `✓ Đã thu thập: ${currentAng.label}`;
            successFlash.classList.remove('hidden');
            setTimeout(() => { successFlash.classList.add('hidden'); }, 800);
        }

        this.renderThumbnailsGrid();
        this.renderAngleStepBadges();

        const count = Object.values(this.capturedImages).filter(Boolean).length;
        if (count >= 1) {
            const btn = document.getElementById('btnSubmitFaceEnroll');
            if (btn) btn.disabled = false;
        }

        const allCompleted = this.anglesConfig.every(ang => Boolean(this.capturedImages[ang.key]));
        if (allCompleted) {
            const tipEl = document.getElementById('guideAngleTip');
            if (tipEl) tipEl.innerHTML = "✅ <strong>Tuyệt vời!</strong> Đã thu thập đủ 3 góc khuôn mặt. Vui lòng bấm <strong>'Lưu Hồ Sơ Face ID'</strong> để hoàn tất.";
            const btn = document.getElementById('btnSubmitFaceEnroll');
            if (btn) {
                btn.disabled = false;
                btn.classList.add('ring-4', 'ring-emerald-400', 'animate-pulse');
            }
        } else {
            // Tự động tìm góc tiếp theo chưa chụp
            let nextIdx = -1;
            for (let i = this.currentStepIndex + 1; i < this.anglesConfig.length; i++) {
                if (!this.capturedImages[this.anglesConfig[i].key]) {
                    nextIdx = i;
                    break;
                }
            }
            if (nextIdx === -1) {
                for (let i = 0; i < this.anglesConfig.length; i++) {
                    if (!this.capturedImages[this.anglesConfig[i].key]) {
                        nextIdx = i;
                        break;
                    }
                }
            }
            if (nextIdx !== -1 && nextIdx !== this.currentStepIndex) {
                setTimeout(() => { this.goToStep(nextIdx); }, 600);
            }
        }
    }

    manualCaptureCurrentAngle() {
        this.captureCurrentAngle();
    }

    dataUrlToBlob(dataUrl) {
        const arr = dataUrl.split(',');
        const mime = arr[0].match(/:(.*?);/)[1];
        const bstr = atob(arr[1]);
        let n = bstr.length;
        const u8arr = new Uint8Array(n);
        while (n--) {
            u8arr[n] = bstr.charCodeAt(n);
        }
        return new Blob([u8arr], { type: mime });
    }

    async submitEnrollment() {
        const count = Object.values(this.capturedImages).filter(Boolean).length;
        if (count < 1) {
            alert("Vui lòng chụp ít nhất góc chính diện!");
            return;
        }

        const btn = document.getElementById('btnSubmitFaceEnroll');
        const btnText = document.getElementById('btnSubmitFaceEnrollText');
        if (btn) btn.disabled = true;
        if (btnText) btnText.textContent = "Đang lưu vector Face ID...";

        try {
            const formData = new FormData();
            if (this.capturedImages.straight) {
                formData.append('image_straight', this.dataUrlToBlob(this.capturedImages.straight), 'straight.jpg');
            }
            if (this.capturedImages.left) {
                formData.append('image_left', this.dataUrlToBlob(this.capturedImages.left), 'left.jpg');
            }
            if (this.capturedImages.right) {
                formData.append('image_right', this.dataUrlToBlob(this.capturedImages.right), 'right.jpg');
            }

            let endpoint = '';
            if (this.currentStudent && this.currentStudent.studentCode) {
                formData.append('studentCode', this.currentStudent.studentCode);
            }
            if (this.currentStudent && this.currentStudent.tempToken) {
                formData.append('tempToken', this.currentStudent.tempToken);
                endpoint = `${this.options.apiBase}/api/auth/enroll-face`;
            } else if (this.currentStudent && this.currentStudent.studentId) {
                endpoint = `${this.options.apiBase}/api/admin/students/${this.currentStudent.studentId}/enroll-face`;
            } else {
                endpoint = `${this.options.faceAiApiBase}/api/v1/face/enroll-multi-angle`;
                formData.append('employee_id', this.currentStudent ? this.currentStudent.studentCode : 'SV001');
                formData.append('full_name', this.currentStudent ? this.currentStudent.fullName : 'Sinh viên');
            }

            const res = await fetch(endpoint, {
                method: 'POST',
                body: formData
            });

            if (res.ok) {
                const resData = await res.json();
                alert("🎉 Đăng ký hồ sơ Face ID 3 góc thành công!");
                this.closeModal();
                if (typeof this.options.onComplete === 'function') {
                    this.options.onComplete(resData);
                }
            } else {
                const errData = await res.json().catch(() => ({}));
                alert("Lỗi khi lưu Face ID: " + (errData.message || res.statusText));
                if (btn) btn.disabled = false;
                if (btnText) btnText.textContent = "Thử Lưu Lại";
            }
        } catch (err) {
            alert("Lỗi kết nối: " + err.message);
            if (btn) btn.disabled = false;
            if (btnText) btnText.textContent = "Lưu Hồ Sơ Face ID";
        }
    }

    async submitVerification() {
        if (this.isSubmitting) return;
        this.isSubmitting = true;

        const video = document.getElementById('faceAiVideo');
        if (!video || !video.videoWidth) {
            this.isSubmitting = false;
            return;
        }

        const btnText = document.getElementById('btnSubmitVerifyText');
        const statusBox = document.getElementById('verifyStatusBox');
        if (btnText) btnText.textContent = "Đang đối chiếu AI...";
        if (statusBox) statusBox.textContent = "⚡ Đang đối chiếu khuôn mặt và điểm danh LMS...";

        const snapCanvas = document.createElement('canvas');
        snapCanvas.width = video.videoWidth;
        snapCanvas.height = video.videoHeight;
        const snapCtx = snapCanvas.getContext('2d');
        snapCtx.drawImage(video, 0, 0, snapCanvas.width, snapCanvas.height);

        snapCanvas.toBlob(async (blob) => {
            if (!blob) {
                this.isSubmitting = false;
                return;
            }

            try {
                const formData = new FormData();
                if (this.currentStudent?.tempToken) {
                    formData.append('tempToken', this.currentStudent.tempToken);
                }
                formData.append('image', blob, 'verify.jpg');

                const res = await fetch(`${this.options.apiBase}/api/auth/verify-face-login`, {
                    method: 'POST',
                    body: formData
                });
                const result = await res.json();

                if (!res.ok) {
                    throw new Error(result.message || 'Khuôn mặt không khớp với hồ sơ!');
                }

                if (statusBox) {
                    statusBox.className = "p-2.5 bg-emerald-950/80 border border-emerald-500 rounded-xl text-center text-xs text-emerald-300 font-bold";
                    statusBox.textContent = "✓ Nhận diện thành công! Đang chuyển hướng...";
                }

                const flash = document.getElementById('faceAiFlashEffect');
                if (flash) {
                    flash.style.opacity = '0.9';
                    setTimeout(() => { flash.style.opacity = '0'; }, 200);
                }

                setTimeout(() => {
                    this.closeModal();
                    if (typeof this.options.onComplete === 'function') {
                        this.options.onComplete(result.data || result);
                    }
                }, 700);

            } catch (err) {
                console.warn("Lỗi xác thực:", err.message);
                if (btnText) btnText.textContent = "Thử Quét Lại";
                if (statusBox) {
                    statusBox.className = "p-2.5 bg-rose-950/80 border border-rose-500 rounded-xl text-center text-xs text-rose-300 font-medium";
                    statusBox.textContent = `Cảnh báo: ${err.message}`;
                }

                if (err.message.includes('chưa có dữ liệu') || err.message.includes('chưa nạp')) {
                    setTimeout(() => { this.switchToEnrollMode(); }, 1800);
                    return;
                }

                this.autoVerifyCooldown = Date.now() + 3000;
                setTimeout(() => {
                    this.isSubmitting = false;
                    if (btnText) btnText.textContent = "Quét & Điểm Danh Vào Học";
                }, 3000);
            }
        }, 'image/jpeg', 0.92);
    }
}

// Khởi tạo Singleton
window.ANGLES_CONFIG = ANGLES_CONFIG;
window.faceAiCaptureInstance = new FaceAiAutoCapture();

window.openFaceEnrollmentModal = function(studentInfo, onSuccessCallback, mode = 'enroll', onSkipCallback = null) {
    if (!window.faceAiCaptureInstance) {
        window.faceAiCaptureInstance = new FaceAiAutoCapture();
    }
    window.faceAiCaptureInstance.options.onComplete = onSuccessCallback;
    window.faceAiCaptureInstance.options.onSkip = onSkipCallback;
    window.faceAiCaptureInstance.open(studentInfo, mode);
};
