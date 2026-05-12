# BurstSlot - High-Concurrency Flash Sale Backend

### 1. Định nghĩa Đề tài

**BurstSlot** là một hệ thống backend được thiết kế chuyên biệt để giải quyết bài toán "Tranh slot" - kịch bản lượng truy cập khổng lồ nhắm vào một lượng tài nguyên cực kỳ giới hạn trong thời gian ngắn (tính bằng mili-giây).
Mục tiêu: **Không bán vượt mức** và **Giữ hệ thống ổn định dưới tải cao**.

### 2. Bối cảnh Môi trường Vận hành

Dự án được thu gọn tối đa dưới dạng MVP để tập trung demo hoàn toàn vào kiến trúc xử lý tương tranh, chạy độc lập trên **Docker Compose** với các thành phần:

- **API Server:** Spring Boot 3.
- **Database:** PostgreSQL 16.
- **RAM Cache:** Redis 7 .
- **Message Broker:** Apache Kafka 3.7.

### 3. Phương pháp Kiểm thử Hiệu năng

#### 3.1. Kịch bản Kiểm thử k6

- **Định nghĩa:** Kịch bản k6 được viết bằng JavaScript, định nghĩa chính xác hành vi của các VUs, bao gồm cấu hình tải, tiêu đề HTTP (Headers), và Payload.
- **Chiến lược:** Cung cấp một kịch bản hoàn chỉnh, trong đó mỗi yêu cầu HTTP được tự động gắn một mã UUID để làm **Idempotency Key**, mô phỏng chính xác các giao dịch độc lập.

**Thực hành: Tạo tệp `loadtest.js` trong thư mục dự án:**

```javascript
import http from "k6/http";
import { check } from "k6";
import { uuidv4 } from "https://jslib.k6.io/k6-utils/1.4.0/index.js";

export const options = {
  scenarios: {
    flash_sale_spike: {
      executor: "shared-iterations",
      vus: 1000,
      iterations: 100000,
      maxDuration: "30s",
    },
  },
};
export default function () {
  const url = "http://localhost:8080/api/v1/bookings";

  const payload = JSON.stringify({
    eventId: 1,
    quantity: 1,
  });

  const params = {
    headers: {
      "Content-Type": "application/json",
      "Idempotency-Key": uuidv4(),
      "X-User-Id": Math.floor(Math.random() * 100000) + 1,
    },
  };

  const response = http.post(url, payload, params);

  check(response, {
    "is status 200 or 409": (r) => r.status === 200 || r.status === 409,
    "is NOT status 500": (r) => r.status !== 500,
  });
}
```

**Thực hành: Khởi chạy kiểm thử:**

Cài k6 trên Ubuntu:

```bash
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6 -y
```

Kiểm thử:

```bash
k6 run loadtest.js
```

#### 3.2. Kiểm tra Kết quả

**Thực hành (SQL Query trên PostgreSQL):**

```sql
SELECT COUNT(*) as total_successful_bookings
FROM reservations
WHERE event_id = 1 AND status = 'CONFIRMED';
```

### 4. Thiết kế Lược đồ Cơ sở dữ liệu Tối ưu

#### 4.1. Bảng Dữ liệu Tĩnh: `events`

```sql
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

#### 4.2. Bảng Trạng thái Tương tranh: `slot`

```sql
CREATE TABLE slot (
    event_id BIGINT PRIMARY KEY REFERENCES events(id),
    available_quantity INTEGER NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_positive_slot CHECK (available_quantity >= 0)
);

CREATE INDEX idx_slot_event_id ON slot(event_id);
```

#### 4.3. Sổ cái Giao dịch: `reservations`

```sql
CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL REFERENCES events(id),
    quantity INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reservations_user_event ON reservations(user_id, event_id);
CREATE INDEX idx_reservations_idempotency ON reservations(idempotency_key);
```
