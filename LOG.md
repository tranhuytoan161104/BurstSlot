# Nhật ký

## **Giai đoạn 1: Spring Boot truyền thống**

- **Hành động:** Sử dụng k6 giả lập 500 người dùng đồng thời tấn công vào luồng cấp phát vé nguyên bản.
- **Sự cố:** Kịch bản kiểm thử làm lộ ra lỗ hổng Lost Update. Do xử lý đọc/ghi tuần tự thiếu cơ chế đồng bộ, các luồng thực thi liên tục ghi đè lên nhau, dẫn đến hiện tượng Overselling hàng ngàn vé.

**Giai đoạn 2: Áp dụng Optimistic Locking (@Version)**

- **Hành động:** Áp dụng cơ chế Optimistic Locking tại tầng Application để ngăn chặn việc ghi đè.
- **Sự cố:** Dữ liệu toàn vẹn, nhưng kiến trúc sụp đổ. Hàng trăm luồng tranh chấp đồng thời sinh ra lượng lớn ngoại lệ từ chối. CPU bị vắt kiệt do phải liên tục xử lý Rollback giao dịch, khiến hệ thống tê liệt.

**Giai đoạn 3: Áp dụng Redis**

- **Hành động:** Đẩy logic phòng thủ ra xa tầng cơ sở dữ liệu (CSDL).
  - **Lớp 1:** Bỏ @Version. Dựng lá chắn Fail-fast bằng RAM sử dụng Redis. Sử dụng phép toán trừ nguyên tử trên RAM để chặn đứng và từ chối ngay lập tức toàn bộ lưu lượng dư thừa.
  - **Lớp 2:** Với lưu lượng hợp lệ, ủy thác hoàn toàn việc cập nhật cho cơ chế Atomic Update của chính PostgreSQL.
- **Kết quả:** k6 xác nhận 500/500 yêu cầu thành công tuyệt đối. Không phát sinh ngoại lệ, Connection Pool được bảo vệ hoàn toàn.

**Giai đoạn 4: Giải quyết bài toán bên thứ 3**

- **Hành động:** Tích hợp Message Broker (Kafka) để xử lý tác vụ gửi email bất đồng bộ.
- **Sự cố:** Dual-write failure. Việc gọi dịch vụ mạng, như Kafka ở đây, khi đang nắm giữ kết nối CSDL tạo ra rủi ro chí mạng. Khi Kafka gặp sự cố, luồng thực thi bị treo, kéo theo việc cạn kiệt toàn bộ hồ chứa kết nối, đánh sập API lõi.
- **Hành động:** Loại bỏ thao tác mạng khỏi giao dịch CSDL bằng mẫu thiết kế Transactional Outbox. Sự kiện được lưu vào một bảng tạm chung giao dịch với đơn hàng, sau đó được một tiến trình ngầm (Worker) xử lý độc lập.
- **Kết quả:** Hệ thống đạt được nhất quán. Khả năng chịu tải vượt mức 11.000 requests/10s, API lõi miễn nhiễm hoàn toàn với các sự cố từ dịch vụ bên thứ ba.
