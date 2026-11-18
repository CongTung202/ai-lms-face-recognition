### 1.2.8. Tìm hiểu về Công nghệ Trí tuệ nhân tạo Nhận diện khuôn mặt (Face AI Core)

#### a. Khái quát về Công nghệ Nhận diện khuôn mặt (Face Recognition)

Trong kỷ nguyên giáo dục số và chuyển đổi số, việc xác thực danh tính người học theo phương thức truyền thống (sử dụng Tên đăng nhập và Mật khẩu) bộc lộ nhiều kẽ hở lớn như chia sẻ tài khoản, điểm danh hộ hay nhờ người thi hộ. Để giải quyết triệt để vấn đề này, hệ thống học trực tuyến EduFace tích hợp công nghệ Nhận diện khuôn mặt (Face Recognition) dựa trên Thị giác máy tính (Computer Vision) và Học sâu (Deep Learning).

Nhận diện khuôn mặt là công nghệ sinh trắc học cho phép tự động phát hiện, trích xuất và đối chiếu danh tính của một cá nhân thông qua hình ảnh hoặc luồng video thời gian thực từ webcam. Công nghệ này hoạt động không tiếp xúc (contactless), không yêu cầu thiết bị phần cứng chuyên dụng đắt tiền, mang lại trải nghiệm tiện lợi và minh bạch tối đa cho người học.

```
[Ảnh từ Webcam] ──► [Phát hiện khuôn mặt] ──► [Căn chỉnh Affine] ──► [Trích xuất Vector 512D] ──► [Đối sánh Cosine]
```

*Hình 1.8. Quy trình nhận diện khuôn mặt tiêu chuẩn trong hệ thống EduFace*

Hai bài toán cốt lõi được hệ thống triển khai bao gồm:

* **Xác thực khuôn mặt (Face Verification - 1:1):** Đối chiếu khuôn mặt hiện tại của sinh viên trước webcam với dữ liệu khuôn mặt đã đăng ký của chính sinh viên đó để kiểm tra danh tính khi vào phòng thi và điểm danh ngẫu nhiên khi đang học.
* **Định danh khuôn mặt (Face Identification - 1:N):** Cho phép sinh viên đăng nhập nhanh chóng bằng khuôn mặt mà không cần nhập mật khẩu. Hệ thống tự động so khớp khuôn mặt trước camera với toàn bộ cơ sở dữ liệu để tìm ra tài khoản tương ứng.

---

#### b. Các mô hình học sâu được ứng dụng trong hệ thống

Thay vì phụ thuộc vào các dịch vụ đám mây thương mại của bên thứ ba (gây tốn kém chi phí và tiềm ẩn nguy cơ rò rỉ dữ liệu sinh trắc học cá nhân), EduFace làm chủ hoàn toàn công nghệ lõi bằng cách triển khai trực tiếp các mô hình Học sâu tiên tiến mã nguồn mở, được nén và tối ưu hóa ở định dạng chuẩn mở **ONNX (Open Neural Network Exchange)** thực thi trên nền tảng **ONNX Runtime**:

##### (1) Mô hình Phát hiện khuôn mặt (Face Detection): YuNet ONNX

* **Nguồn gốc:** Do nhóm nghiên cứu của Giáo sư Shiqi Yu phát triển và được tích hợp chính thức vào thư viện OpenCV Zoo (`cv2.FaceDetectorYN`).
* **Trọng số triển khai:** Tập tin `face_detection_yunet.onnx` với kích thước siêu nhỏ gọn chỉ khoảng **232 KB**.
* **Kiến trúc:** Mạng tích chập sâu siêu nhẹ 1-giai đoạn (Single-Stage CNN) theo cơ chế không neo (Anchor-free), loại bỏ các phép tính trùng lặp dư thừa.
* **Chức năng:** Tự động phát hiện vị trí khuôn mặt trong khung hình với tốc độ cực cao (>100 khung hình/giây trên CPU thông thường), xuất ra:
  * Tọa độ khung bao khuôn mặt ($Bounding\ Box: x, y, w, h$).
  * Điểm tin cậy phát hiện ($Confidence\ score$).
  * Tọa độ của **5 điểm mốc giải phẫu khuôn mặt (Facial Landmarks)**: Mắt phải, mắt trái, chóp mũi, khóe miệng phải và khóe miệng trái.

##### (2) Mô hình Trích xuất đặc trưng sinh trắc học (Face Embedding): ArcFace MobileFaceNet

* **Nguồn gốc:** Kiến trúc **MobileFaceNet** kết hợp cùng hàm mất mát góc **ArcFace (Additive Angular Margin Loss)** do nhóm nghiên cứu Imperial College London / InsightFace công bố tại hội nghị CVPR.
* **Trọng số triển khai:** Tập tin `w600k_mbf.onnx` (dung lượng xấp xỉ **13.6 MB**), được huấn luyện chuyên sâu trên tập dữ liệu chuẩn thế giới **WebFace600K** với hơn 600.000 danh tính khác nhau.
* **Kiến trúc:** Tối ưu hóa cấu trúc tích chập tách biệt theo chiều sâu (Depthwise Separable Convolutions) kết hợp lớp tích chập sâu toàn cục (GDConv), giảm số lượng tham số xuống chỉ còn ~1.2 triệu (so với hơn 60 triệu tham số của ResNet-100), cho phép trích xuất đặc trưng cực nhanh trên máy chủ CPU.
* **Chức năng:** Nhận ảnh khuôn mặt đã căn chỉnh kích thước chuẩn $112 \times 112$ pixel và nén toàn bộ thông tin sinh trắc học độc nhất của sinh viên thành một **vector số thực 512 chiều (512-dimensional embedding vector)**.

##### (3) Mô hình Ước lượng tư thế khuôn mặt 3 chiều (Head Pose 3D)

* **Phương pháp:** Tích hợp giải thuật hình học không gian 3 chiều dựa trên 5 điểm mốc nhân trắc học khuôn mặt (`head_pose_3d.py`).
* **Chức năng:** Đo đạc chính xác 3 góc xoay Euler trong thời gian thực:
  * Góc xoay ngang (**Yaw**): Phát hiện sinh viên quay đầu sang trái (`TURN_LEFT`) hoặc sang phải (`TURN_RIGHT`) để nhìn bài.
  * Góc cúi/ngửa (**Pitch**): Phát hiện sinh viên cúi đầu xuống dưới (`LOOK_DOWN`) để xem điện thoại hoặc tài liệu.
  * Góc nghiêng (**Roll**): Đo độ nghiêng đầu theo trục ngang.

---

#### c. Các thuật toán và cơ sở toán học cốt lõi

##### (1) Thuật toán Căn chỉnh khuôn mặt (Face Alignment via Affine Transformation)

Để loại bỏ các sai lệch do sinh viên ngồi nghiêng đầu trước máy tính, hệ thống sử dụng thuật toán biến đổi afin bảo toàn tỉ lệ (Similarity Transformation) nhằm đưa 5 điểm mốc thực tế về hệ tọa độ chuẩn trên mặt phẳng ảnh $112 \times 112$ pixel:

* Tập 5 điểm tọa độ chuẩn $\mathbf{Y}_{\text{ref}}$:
  $$\mathbf{Y}_{\text{ref}} = \begin{bmatrix} 38.29 & 51.70 \\ 73.53 & 51.50 \\ 56.03 & 71.74 \\ 41.55 & 92.37 \\ 70.73 & 92.20 \end{bmatrix}$$
* Thuật toán tính toán ma trận afin $\mathbf{M} \in \mathbb{R}^{2 \times 3}$ và thực hiện phép biến đổi nội suy ảnh, đảm bảo hai mắt của sinh viên luôn nằm trên một đường thẳng ngang hoàn hảo trước khi đưa vào mạng trích xuất đặc trưng.

##### (2) Thuật toán Tối ưu hóa góc ArcFace Loss (Additive Angular Margin Loss)

ArcFace là hàm mất mát đột phá giúp mô hình phân biệt danh tính sinh viên với độ chính xác cao vượt trội. Hàm mục tiêu được định nghĩa như sau:
$$L_{\text{ArcFace}} = -\log \frac{e^{s \cdot \cos(\theta_{y_i} + m)}}{e^{s \cdot \cos(\theta_{y_i} + m)} + \sum_{j \neq y_i}^N e^{s \cdot \cos(\theta_j)}}$$

*Trong đó:*

* $s = 64$: Tham số tỷ lệ bán kính siêu cầu.
* $m = 0.5$ radian ($\approx 28.6^\circ$): Biên độ khoảng cách góc cộng dồn.
* **Ý nghĩa:** ArcFace cưỡng bức góc giữa các bức ảnh của cùng một người (nội lớp) phải co hẹp lại, đồng thời nới rộng khoảng cách góc giữa các sinh viên khác nhau (liên lớp), tạo nên không gian vector đặc trưng có khả năng phân biệt cực đại.

##### (3) Thuật toán Đo độ tương đồng Cosine (Cosine Similarity)

Sau khi vector đặc trưng $\mathbf{v} \in \mathbb{R}^{512}$ được chuẩn hóa theo độ dài $L_2$ ($\|\mathbf{v}\|_2 = 1$), độ tương đồng giữa hai khuôn mặt $\mathbf{u}$ và $\mathbf{v}$ được tính toán trực tiếp bằng tích vô hướng:
$$\text{Similarity}(\mathbf{u}, \mathbf{v}) = \hat{\mathbf{u}} \cdot \hat{\mathbf{v}} = \sum_{k=1}^{512} \hat{u}_k \hat{v}_k$$

* **Ngưỡng quyết định (Threshold):** Được thiết lập mặc định ở mức **0.60** (tương đương độ chính xác xác thực đạt trên 99.2%). Nếu giá trị $\ge 0.60$, hệ thống xác nhận chính xác sinh viên; ngược lại sẽ từ chối truy cập.

##### (4) Thuật toán Chống giả mạo sinh trắc học thụ động (Passive Anti-Spoofing)

Nhằm ngăn chặn hành vi sử dụng ảnh in màu trên giấy hoặc dùng điện thoại/máy tính bảng chiếu ảnh để điểm danh hộ, hệ thống ứng dụng giải thuật phân tích đa nhân tố:

* **Phân tích kết cấu tần số cao (Moiré Pattern Analysis):** Màn hình điện tử phát xạ ánh sáng qua lưới điểm ảnh ma trận, khi camera quay lại sẽ tạo ra các vân giao thoa Moiré bất thường. Thuật toán sử dụng toán tử vi phân bậc hai Laplace $\nabla^2 I$ để phát hiện sự sai khác giữa da người thật và màn hình phát lại.
* **Phân tích sắc tố da trong không gian YCrCb:** Da người tự nhiên phản xạ phổ màu sắc tố tập trung trong dải hẹp $130 \le Cr \le 175$ và $75 \le Cb \le 130$. Ảnh chụp lại qua màn hình sẽ bị lệch màu rõ rệt.
* **Phát hiện phản xạ lóa bề mặt gương (Specular Glare):** Bề mặt kính màn hình điện thoại tạo ra các điểm chói lóa cường độ bão hòa ($V > 250$ trong không gian màu HSV).
* Điểm số Liveness tổng hợp phải thỏa mãn $\text{Score}_{\text{Liveness}} \ge 0.55$ thì hệ thống mới chấp nhận kết quả nhận diện.

##### (5) Thuật toán Chống thi hộ chéo (Anti-Impersonation Check)

Khi một thí sinh điểm danh vào bài thi dưới mã sinh viên $A$, hệ thống không chỉ so khớp 1:1 với hồ sơ của $A$, mà đồng thời kích hoạt quét tìm kiếm 1:N với toàn bộ cơ sở dữ liệu sinh viên trong trường. Nếu khuôn mặt trước camera trùng khớp với sinh viên $B$ với độ tin cậy cao hơn, hệ thống ngay lập tức kích hoạt mã cảnh báo `IMPERSONATION_BLOCKED`, từ chối phiên làm bài và lưu lại bằng chứng gian lận.

---

#### d. Các ứng dụng thực tế đã triển khai công nghệ này

Công nghệ nhận diện khuôn mặt sinh trắc học đã được chứng minh hiệu quả và ứng dụng rộng khắp trên toàn thế giới cũng như tại Việt Nam:

*Bảng 1.4. Một số ứng dụng tiêu biểu của công nghệ nhận diện khuôn mặt*

| Lĩnh vực | Ứng dụng tiêu biểu | Mô tả chức năng thực tế |
| :--- | :--- | :--- |
| **Giáo dục & Thi cử trực tuyến** | **Proctorio, LockDown Browser, Moodle Plugin** | Giám sát kỳ thi trực tuyến tự động cho các đại học danh tiếng thế giới (Harvard, edX, Coursera). Khóa trình duyệt, nhận diện thí sinh và cảnh báo rời màn hình. |
| **Định danh điện tử (eKYC) & Ngân hàng** | **Apple Face ID, VNPT eKYC, FPT.AI eKYC** | Mở khóa điện thoại bảo mật cao; hỗ trợ các ngân hàng (MBBank, Techcombank, Vietcombank) mở tài khoản trực tuyến từ xa và chống giả mạo sinh trắc học khi giao dịch. |
| **Quản trị nhân sự & Chấm công** | **Hikvision Face Terminal, Suprema, Base Checkin** | Chấm công không tiếp xúc tại các tòa nhà văn phòng, khu công nghiệp và trường học bằng camera AI. |
| **An ninh hàng không & Biên giới** | **Cổng thông minh Smart Gates (Changi, Nội Bài)** | So khớp hộ chiếu điện tử với khuôn mặt hành khách để tự động xuất nhập cảnh trong 3 giây. |

---

#### e. Những công trình nghiên cứu khoa học nền tảng liên quan

Phân hệ Face AI Core của EduFace được xây dựng dựa trên việc kế thừa và tổng hợp các công trình nghiên cứu khoa học mang tính bước ngoặt:

1. **Về Phát hiện khuôn mặt (Face Detection):**
   * *Viola & Jones (CVPR 2001):* Nghiên cứu nền tảng về thuật toán Haar-Cascade thời gian thực.
   * *Kaipeng Zhang et al. (IEEE SPL 2016):* Đột phá mạng CNN xếp tầng MTCNN kết hợp phát hiện mặt và định vị 5 điểm mốc.
   * *Shiqi Yu et al. (OpenCV Zoo 2021):* Công trình phát triển mô hình YuNet siêu nhẹ cho thiết bị biên và CPU, được tích hợp trực tiếp vào hệ thống EduFace.
2. **Về Trích xuất đặc trưng và Hàm mất mát (Metric Learning & Loss Functions):**
   * *Florian Schroff et al. (Google, CVPR 2015):* Công bố mạng FaceNet và giải thuật Triplet Loss, ánh xạ mặt người vào không gian Euclid.
   * *Jiankang Deng et al. (InsightFace, CVPR 2019):* Công bố giải thuật **ArcFace (Additive Angular Margin Loss)**, thiết lập chuẩn mực độ chính xác mới (>99.8% trên tập dữ liệu LFW).
   * *Sheng Chen et al. (CCBR 2018):* Thiết kế mạng **MobileFaceNets** siêu nhẹ, tối ưu hóa cho môi trường di động và máy chủ CPU.
3. **Về Chống giả mạo sinh trắc học (Anti-Spoofing):**
   * *Ivana Chingovska et al. (IEEE BIOSIG 2012):* Nghiên cứu hiệu quả của phân tích hoa văn vi mô kết cấu (Local Binary Patterns) trong việc phân biệt da người sống với ảnh in và màn hình phẳng.
4. **Về Giám thị thi trực tuyến thông minh (Smart Exam Proctoring):**
   * *Yousef Atoum et al. (IEEE Transactions on Multimedia 2017):* Đề xuất kiến trúc tự động hóa giám thị kỳ thi trực tuyến qua webcam, phát hiện hành vi nhìn bài và tráo người thi.
