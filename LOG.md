# Nhật ký Phát triển & Thực nghiệm Hệ thống (BurstSlot Evolution)

Tài liệu này ghi lại quá trình tiến hóa của kiến trúc hệ thống BurstSlot từ mức cơ bản đến cấp độ Enterprise, kèm theo các số liệu thực chứng từ công cụ kiểm thử tải (Load testing) k6.

---

## Stage 1: Spring Boot Truyền thống (Naive Method)

### Kịch bản thực nghiệm

Hệ thống được thiết lập với **500 vé (slots)**. Sử dụng k6 giả lập 500 luồng người dùng (VUs) liên tục bắn tải vào API đặt vé trong 10 giây.

### Kết quả (Số liệu thực chứng)

- **Thông lượng (Throughput):** ~427 req/s
- **Dữ liệu thực tế:** Hệ thống bán thành công **4.725 vé** (trong khi kho chỉ có 500). Kho vé bị thủng về mức âm 4.225. Thảm họa Overselling (Bán vượt mức) đã xảy ra.

### Phân tích kỹ thuật (Lý do)

- **Bản chất vấn đề:** Ứng dụng xử lý theo logic nguyên bản: `Tìm vé -> Kiểm tra số lượng -> Trừ vé -> Lưu`. Đây là mô hình Đọc/Ghi tuần tự thiếu cơ chế đồng bộ (Locking).
- **Hiện tượng Lost Update:** Khi 500 luồng đồng thời gọi `findById`, tất cả đều nhìn thấy kho đang còn 500 vé. Mọi luồng đều lọt qua vòng kiểm tra và thi nhau ghi đè kết quả `save(499)` xuống cơ sở dữ liệu. Hàng ngàn giao dịch đã thành công trên cùng một tài nguyên đĩa cứng.

---

## Stage 2: Phòng thủ CSDL với Optimistic Locking (`@Version`)

### Giải pháp

Áp dụng cơ chế Khóa Lạc quan (Optimistic Locking) bằng annotation `@Version` của JPA tại tầng Application để ngăn chặn việc ghi đè dữ liệu tàn bạo ở Stage 1.

### Kết quả (Số liệu thực chứng)

- **Thông lượng (Throughput):** ~968 req/s
- **Dữ liệu thực tế:** Hệ thống bán ra chuẩn xác tuyệt đối **500 vé**. Kho hàng vừa vặn về 0. Không có vé nào bị bán vượt.
- **Sự đánh đổi:** Có tới **10.557 requests bị từ chối** (Failed) ngay tại tầng ứng dụng.

### Phân tích kỹ thuật (Lý do)

- **Thành công về Dữ liệu:** Khi một luồng cập nhật thành công, `version` trong CSDL tăng lên. 499 luồng còn lại mang `version` cũ xuống xin ghi đè sẽ bị PostgreSQL và JPA thẳng tay từ chối. Dữ liệu được bảo vệ an toàn tuyệt đối.
- **Sụp đổ về Kiến trúc dưới tải cao:** Dù dữ liệu đúng, nhưng việc từ chối 10.557 requests đồng nghĩa với việc sinh ra **10.557 ngoại lệ `OptimisticLockingFailureException`** trong RAM. Quá trình xử lý Exception trong Java và hoàn tác giao dịch (Rollback Transaction) ở PostgreSQL tiêu tốn khổng lồ CPU. Dưới tải siêu cao (Flash Sale thực tế), DB sẽ cạn kiệt tài nguyên tính toán và sập nguồn. Phương án này "Đúng" nhưng "Không Tối Ưu".

---

## Stage 3: Phân tán tải trọng với Redis (Fail-Fast Pattern)

### Giải pháp

Đẩy lùi logic phòng thủ ra xa tầng cơ sở dữ liệu để bảo vệ Connection Pool. Sử dụng phép toán trừ nguyên tử (`decrement`) của Redis trên RAM làm lớp khiên đầu tiên (Lớp 2 vẫn là `@Version` của CSDL để đề phòng rủi ro).

### Kết quả (Số liệu thực chứng)

- **Thông lượng (Throughput):** Đạt mức cực đại **~1.690 req/s** (Gấp 4 lần Stage 1, gần gấp đôi Stage 2).
- **Dữ liệu thực tế:** Bán chuẩn xác **500 vé**. Tổng số requests bị từ chối lên tới 18.585.

### Phân tích kỹ thuật (Lý do cốt lõi)

- **Sự khác biệt tạo nên nhảy vọt (Cùng hệ quy chiếu):** Tốc độ tăng vọt này hoàn toàn là công lao của Redis. Hơn 18.500 requests đến muộn (khi vé đã hết) đã bị Redis vứt bỏ ngay trên RAM trong tích tắc.
- **Bảo vệ CSDL:** Vì rác bị chặn ở cửa, chúng **không bao giờ được phép chạm tới PostgreSQL**, triệt tiêu hoàn toàn cơn bão Exception và Rollback khổng lồ của Stage 2. Hệ thống duy trì được CPU ở mức thấp dù bị spam 19.000 requests trong vài giây.

---

## Stage 4: Tích hợp API Bên thứ 3 (Kafka + Outbox Pattern)

### Vấn đề

Khi thêm nghiệp vụ "Gửi Email" vào luồng đặt vé, nếu thực hiện gọi hàm gửi Email trực tiếp (Đồng bộ), quá trình chờ mạng sẽ giam lỏng (hold) Connection CSDL. Dưới tải cao của k6, CSDL sẽ ngay lập tức sập do cạn kiệt Connection Pool (Dual-write failure).

### Giải pháp & Bản chất kỹ thuật

- **Cắt đuôi nghiệp vụ:** Sử dụng Transactional Outbox Pattern + Kafka.
- Luồng giao dịch chính (đặt vé) không thèm gửi Email. Nó chỉ viết 1 dòng trạng thái "PENDING" vào bảng Outbox rồi đóng giao dịch lại và báo thành công cho người dùng.
- Một công nhân (Worker) chạy ngầm sẽ tự động gom các dòng "PENDING" đẩy lên Kafka để một dịch vụ khác lo việc gửi mail.
- **Kết quả:** Luồng đặt vé lõi tiếp tục giữ được tốc độ xử lý siêu tốc, hoàn toàn cách ly và miễn nhiễm với sự cố sập mạng hoặc chậm trễ từ các máy chủ Email bên ngoài.
