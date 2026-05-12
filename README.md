### 1. Định nghĩa Đề tài (Project Definition)

**BurstSlot** là một hệ thống backend xử lý giao dịch đồng thời cao (High-Concurrency Transactional Backend). Hệ thống này được thiết kế chuyên biệt để giải quyết bài toán "Flash Sale" hoặc "Tranh slot" — một kịch bản trong đó một khối lượng truy cập khổng lồ (massive traffic spike) đột ngột nhắm vào một tài nguyên chia sẻ có số lượng cực kỳ giới hạn (ví dụ: vé sự kiện, tín chỉ học tập) trong một cửa sổ thời gian tính bằng mili-giây, đòi hỏi hệ thống phải đảm bảo tính toàn vẹn dữ liệu tuyệt đối (không cho phép bán vượt mức - overbooking) mà không bị sụp đổ do cạn kiệt tài nguyên máy chủ.

### 2. Bối cảnh Môi trường Thiết đặt và Vận hành (Deployment & Runtime Context)

Hệ thống BurstSlot không chạy dưới dạng một tiến trình đơn lẻ trên một máy tính cá nhân, mà được thiết đặt trong một kiến trúc hướng đám mây (Cloud-Native Architecture). Kiến trúc hướng đám mây là một phương pháp xây dựng và chạy các ứng dụng khai thác lợi thế của mô hình phân phối điện toán đám mây, bao gồm các môi trường động như các đám mây công cộng, riêng tư và lai.

- **Tầng Ảo hóa và Điều phối (Virtualization & Orchestration):** Ứng dụng được đóng gói bằng **Docker** (một nền tảng phần mềm cho phép tạo, kiểm thử và triển khai các ứng dụng một cách nhanh chóng dưới dạng các container - vùng chứa độc lập, chuẩn hóa) và được điều phối bởi **Kubernetes** (một nền tảng mã nguồn mở có khả năng tự động hóa việc triển khai, mở rộng quy mô và quản lý các ứng dụng được chứa trong container).
- **Môi trường Thực thi (Runtime Environment):** Sử dụng **Java Virtual Machine (JVM) phiên bản 21 trở lên**. JVM là một cỗ máy ảo cho phép máy tính chạy các chương trình Java cũng như các chương trình viết bằng ngôn ngữ khác được biên dịch sang Java bytecode. Phiên bản 21 được chọn để kích hoạt **Virtual Threads** (Luồng ảo - một tính năng cung cấp các luồng nhẹ do JVM quản lý thay vì hệ điều hành quản lý, giúp xử lý hàng vạn kết nối đồng thời với chi phí bộ nhớ cực thấp).
- **Môi trường Cơ sở dữ liệu (Database Environment):**
  - **PostgreSQL:** Hoạt động trong cụm **Primary-Replica** (Chủ-Tớ). Primary-Replica là kiến trúc nơi một máy chủ cơ sở dữ liệu (Primary) xử lý toàn bộ các lệnh ghi dữ liệu, và liên tục sao chép dữ liệu đó sang một hoặc nhiều máy chủ khác (Replica) chỉ để phục vụ các lệnh đọc, giúp giảm tải cho máy chủ chính.
  - **Redis Cluster:** Hoạt động dưới dạng một cụm phân tán bộ nhớ đệm. Redis Cluster là một cấu trúc dữ liệu lưu trữ trong bộ nhớ RAM, được phân chia trên nhiều nút (nodes) mạng để đảm bảo tính sẵn sàng cao và khả năng mở rộng ngang, được dùng để quản lý khóa và lưu trữ trạng thái tạm thời.
- **Tầng Giao tiếp Liên tiến trình (Inter-Process Communication):** Sử dụng **Apache Kafka**. Kafka là một nền tảng truyền phát sự kiện phân tán (distributed event streaming platform) có khả năng đọc, ghi, lưu trữ và xử lý các luồng sự kiện theo thời gian thực một cách bền bỉ và có khả năng chịu lỗi cao.
- **Tầng Cổng giao tiếp (API Gateway):** Sử dụng **Spring Cloud Gateway**. API Gateway là một máy chủ đóng vai trò là điểm vào duy nhất vào hệ thống, định tuyến các yêu cầu từ máy khách (client) đến các vi dịch vụ (microservices) backend phù hợp.

---

### 3. Chiến lược Xử lý các Yếu tố Cốt lõi

Các yếu tố dưới đây định hình năng lực kỹ thuật của hệ thống.

#### 3.1. Hiệu năng cao (High Performance)

- **Định nghĩa:** Hiệu năng cao là khả năng của hệ thống trong việc hoàn thành một khối lượng công việc lớn (thông lượng - throughput cao) với thời gian phản hồi (độ trễ - latency) thấp nhất có thể, sử dụng tài nguyên phần cứng một cách tối ưu.
- **Chiến lược:** Kích hoạt Virtual Threads (Luồng ảo) của Java 21 để giảm thiểu chi phí chuyển đổi ngữ cảnh (Context switching - quá trình lưu và khôi phục trạng thái của một CPU để nhiều tiến trình có thể chia sẻ cùng một tài nguyên CPU duy nhất) của hệ điều hành. Đồng thời, thiết lập cơ chế Caching (Bộ nhớ đệm - nơi lưu trữ dữ liệu tạm thời để phục vụ các yêu cầu trong tương lai nhanh hơn) nhiều tầng bằng Redis để chặn 90% các truy vấn đọc không cần thiết chạm đến CSDL PostgreSQL.

#### 3.2. Xử lý Race condition (Trạng thái tương tranh)

- **Định nghĩa:** Trạng thái tương tranh là một hành vi dị thường của hệ thống xảy ra khi nhiều luồng hoặc tiến trình cùng truy cập và thao tác trên một dữ liệu chia sẻ cùng lúc, và kết quả cuối cùng của dữ liệu bị sai lệch do phụ thuộc vào thứ tự thực thi ngẫu nhiên của các luồng.
- **Chiến lược:** Áp dụng thuật toán Distributed Lock (Khóa phân tán) thông qua thư viện Redisson để tuần tự hóa (đưa vào hàng đợi xử lý lần lượt) các yêu cầu tranh giành cùng một `slot_id` tại tầng ứng dụng. Sau đó, cấu hình Optimistic Locking (Khóa lạc quan - cơ chế ngăn chặn ghi đè dữ liệu bằng cách kiểm tra số phiên bản dữ liệu trước khi cập nhật) tại tầng PostgreSQL để làm chốt chặn bảo vệ cuối cùng, đảm bảo dữ liệu nguyên vẹn ngay cả khi khóa phân tán gặp sự cố mạng.

#### 3.3. Đa luồng (Multithreading)

- **Định nghĩa:** Đa luồng là một mô hình thực thi lập trình cho phép một tiến trình (process) đơn lẻ sinh ra nhiều luồng (threads) hoạt động đồng thời, chia sẻ cùng một không gian bộ nhớ để tăng cường khả năng đáp ứng và thông lượng tính toán.
- **Chiến lược:** Cấu hình thư viện HikariCP (một bộ quản lý hồ chứa kết nối - Connection Pool cực kỳ nhẹ và nhanh) một cách nghiêm ngặt nhằm duy trì trạng thái cân bằng. Cụ thể, số lượng luồng ảo tiếp nhận yêu cầu có thể lên tới hàng ngàn, nhưng số lượng kết nối đồng thời đẩy xuống PostgreSQL phải bị giới hạn chặt chẽ dựa trên số lượng lõi (core) vật lý của máy chủ cơ sở dữ liệu, ngăn chặn hiện tượng Thundering Herd (Bầy đàn sấm sét - khi hàng nghìn luồng cùng lúc đánh thức CSDL gây cạn kiệt tài nguyên).

#### 3.4. Song song (Parallelism)

- **Định nghĩa:** Tính song song là khả năng của phần cứng (thường là CPU đa lõi) chia nhỏ một khối lượng công việc khổng lồ thành các phần độc lập và thực thi chúng thực sự cùng một lúc tại cùng một thời điểm vật lý.
- **Chiến lược:** Tuyệt đối không áp dụng xử lý song song trong luồng giao dịch cốt lõi (luồng đặt slot) để tránh gia tăng chi phí đồng bộ hóa dữ liệu. Tính song song chỉ được áp dụng ở các tác vụ ngoại vi (background jobs), ví dụ sử dụng Java Parallel Streams (Luồng song song trong Java) để tổng hợp nhanh hàng triệu báo cáo đối soát giao dịch định kỳ vào cuối ngày, khi hệ thống ít chịu tải nhất.

#### 3.5. Phân tán (Distributed)

- **Định nghĩa:** Hệ thống phân tán là một mạng lưới bao gồm nhiều máy tính độc lập về mặt vật lý nhưng xuất hiện trước người dùng như một hệ thống máy tính duy nhất và thống nhất, phối hợp với nhau thông qua việc gửi thông điệp (message passing).
- **Chiến lược:** Chuyển đổi mô hình xử lý đồng bộ (Synchronous - tiến trình gọi phải chờ tiến trình được gọi hoàn thành) sang bất đồng bộ (Asynchronous - tiến trình gọi tiếp tục công việc ngay lập tức mà không chờ đợi). Cụ thể, sau khi trừ slot thành công trong CSDL, hệ thống không gọi trực tiếp dịch vụ gửi email hay trừ tiền, mà đẩy một thông điệp (message) vào Apache Kafka. Các máy chủ khác trong cụm phân tán sẽ tự động kéo thông điệp này về xử lý sau, giải phóng ngay lập tức luồng giao dịch chính.

#### 3.6. Microservices (Vi dịch vụ)

- **Định nghĩa:** Kiến trúc Vi dịch vụ là một phương pháp luận kỹ thuật phần mềm cấu trúc một ứng dụng thành một tập hợp các dịch vụ nhỏ, liên kết lỏng lẻo (loosely coupled), có thể triển khai độc lập và được tổ chức xung quanh các khả năng kinh doanh cụ thể.
- **Chiến lược:** Phân rã bài toán thành các miền độc lập. BurstSlot được thiết kế ít nhất với hai dịch vụ: `SlotService` (chịu trách nhiệm giữ và khóa dữ liệu slot) và `BookingService` (chịu trách nhiệm xác thực người dùng và nhận yêu cầu). Việc này cho phép mở rộng quy mô ngang (Scale-out - thêm máy chủ mới) chỉ riêng cho `BookingService` khi lưu lượng truy cập tăng đột biến, giúp tiết kiệm tài nguyên.

#### 3.7. Sử dụng các thư viện phổ biến đúng cách

- **Định nghĩa:** Việc áp dụng một cách chuẩn xác, có tính toán các đoạn mã nguồn đã được cộng đồng kiểm chứng (libraries/frameworks), tuân thủ nghiêm ngặt các nguyên tắc thiết kế gốc của tác giả thư viện để giải quyết các vấn đề chung mà không phải "phát minh lại bánh xe".
- **Chiến lược:** Sử dụng Spring Data JPA (một công cụ ánh xạ đối tượng-quan hệ) một cách tinh gọn, tránh các lỗi phổ biến như truy vấn N+1 (một lỗi hiệu suất khi một truy vấn ban đầu kéo theo N truy vấn phụ khác). Ứng dụng Flyway (một công cụ quản lý thay đổi cơ sở dữ liệu) để tự động hóa việc nâng cấp và kiểm soát phiên bản của các bảng (tables) trong PostgreSQL, đảm bảo tính nhất quán của cấu trúc dữ liệu trên mọi môi trường từ lập trình viên đến máy chủ sản xuất (production).

#### 3.8. Bảo mật phổ biến (Popular Security)

- **Định nghĩa:** Việc áp dụng các cơ chế, chính sách và tiêu chuẩn công nghiệp (như OWASP - Dự án Bảo mật Ứng dụng Web Mở) nhằm bảo vệ tính toàn vẹn, tính bảo mật và tính khả dụng của dữ liệu khỏi các mối đe dọa không gian mạng trực diện.
- **Chiến lược:** Xác thực các yêu cầu API bằng JSON Web Token (JWT - một chuẩn mở định nghĩa cách thức truyền thông tin an toàn, nhỏ gọn dưới dạng đối tượng JSON) kết hợp thuật toán mã hóa phi đối xứng. Đặc biệt, thiết lập Rate Limiting (Giới hạn tốc độ) trên API Gateway để ngăn chặn các cuộc tấn công DDoS (Tấn công từ chối dịch vụ phân tán - nỗ lực làm sập máy chủ bằng cách làm tràn ngập lưu lượng truy cập rác), tự động từ chối các IP gửi quá số lượng yêu cầu cho phép trong 1 giây.

#### 3.9. Thuật toán/Design pattern phổ biến

- **Định nghĩa:** Design Pattern (Mẫu thiết kế) là các giải pháp tổng quát, có thể tái sử dụng để giải quyết các vấn đề phổ biến xảy ra trong thiết kế phần mềm. Thuật toán (Algorithm) là một tập hợp các hướng dẫn toán học hoặc logic hữu hạn để giải quyết một lớp vấn đề tính toán cụ thể.
- **Chiến lược:** Cài đặt mẫu thiết kế Idempotency (Tính lũy đẳng - tính chất đảm bảo rằng việc áp dụng một thao tác nhiều lần có kết quả hệ thống giống như áp dụng thao tác đó một lần duy nhất). Sử dụng Redis để lưu trữ một khóa duy nhất cho mỗi giao dịch; nếu người dùng bị lỗi mạng và nhấn nút "Đặt" 10 lần trong 1 giây, hệ thống vẫn nhận diện đó là 1 yêu cầu duy nhất và không thực hiện trừ dữ liệu 10 lần.

#### 3.10. Yếu tố quan trọng khác: Observability (Khả năng quan sát)

- **Định nghĩa:** Khả năng quan sát là năng lực đo lường và thấu hiểu trạng thái bên trong của một hệ thống công nghệ thông tin phức tạp chỉ bằng cách kiểm tra các đầu ra bên ngoài của nó, cụ thể là Logs (Nhật ký), Metrics (Số liệu) và Traces (Truy vết).
- **Chiến lược:** Hệ thống bắt buộc phải tích hợp thư viện Micrometer và định dạng log chuẩn hóa cấu trúc (JSON Log). Mọi yêu cầu đi vào hệ thống đều được cấp một TraceID (Mã định danh truy vết - một chuỗi ký tự duy nhất dùng để theo dõi hành trình của một yêu cầu đi qua toàn bộ các microservices), giúp quá trình gỡ lỗi (debugging) trạng thái tương tranh hoặc lỗi dữ liệu trở nên minh bạch và có cơ sở khoa học.

### 4. Danh sách Tính năng và APIs cốt lõi (Core Features & APIs MVP)

**API Endpoint (Điểm cuối API):** Là một điểm định tuyến cụ thể (thường là một URL kỹ thuật số) trên máy chủ, nơi hệ thống tiếp nhận yêu cầu từ ứng dụng khách, thực thi logic nghiệp vụ và trả về kết quả tương ứng.

Dự án BurstSlot được thu gọn tối đa vòng đời phát triển, chỉ tập trung vào 3 API RESTful và 1 tính năng xử lý nền để chứng minh năng lực thiết kế.

#### 4.1. API Khởi tạo Sự kiện (Event Initialization API)

- **Tên/Đường dẫn:** `POST /api/v1/events`
- **Mô tả:** Chức năng thiết lập một phiên Flash Sale mới, ghi nhận thông tin cơ bản và nạp tổng số lượng slot (chỗ trống) giới hạn ban đầu vào hệ thống lưu trữ.
- **Đóng góp chiến lược:**
  - **Bảo mật phổ biến:** Áp dụng phân quyền chặt chẽ (Role-Based Access Control), chỉ tài khoản Quản trị viên (Admin) mang chuỗi xác thực JWT hợp lệ mới có quyền gọi API này.
  - **Microservices:** Khởi tạo dữ liệu gốc cho miền độc lập là SlotService.

#### 4.2. API Truy vấn Trạng thái Slot (Slot Status Query API)

- **Tên/Đường dẫn:** `GET /api/v1/events/{eventId}/slots`
- **Mô tả:** Trả về số lượng slot còn lại tại thời gian thực. Đây là điểm chịu tải đọc (Read-heavy) khổng lồ do hành vi liên tục tải lại trang (F5) của hàng ngàn người dùng trước giờ "mở bán".
- **Đóng góp chiến lược:**
  - **Hiệu năng cao:** Giải quyết triệt để rào cản tốc độ bằng cách áp dụng bộ nhớ đệm (Caching). Toàn bộ truy vấn đọc được chặn và trả kết quả trực tiếp từ RAM thông qua cụm Redis Cluster, giảm tải 99% áp lực truy xuất cho đĩa cứng của cụm PostgreSQL.

#### 4.3. API Giao dịch Đặt chỗ (Core Booking API)

- **Tên/Đường dẫn:** `POST /api/v1/bookings`
- **Mô tả:** Điểm thắt cổ chai cốt lõi của toàn bộ hệ thống (Write-heavy bottleneck). API này đồng thời tiếp nhận hàng vạn yêu cầu tranh giành cùng một slot vật lý trong cùng một mili-giây.
- **Đóng góp chiến lược:** Đây là tâm điểm hội tụ kỹ thuật, bao gồm:
  - **Xử lý Race condition:** Tạo ra lớp phòng thủ kép. Lớp 1 dùng Khóa phân tán (Distributed Lock) trên Redis để tuần tự hóa các yêu cầu tại RAM. Lớp 2 dùng Khóa lạc quan (Optimistic Locking) dưới PostgreSQL làm chốt chặn vĩnh viễn chống ghi đè dữ liệu.
  - **Đa luồng:** Tận dụng Virtual Threads (Luồng ảo) để cho phép máy chủ tiếp nhận hàng chục ngàn kết nối HTTP đang chờ khóa mà không bị sập do tràn bộ nhớ (OutOfMemoryError).
  - **Design pattern phổ biến:** Ép buộc máy khách đính kèm một mã khóa duy nhất vào tiêu đề (Header). Hệ thống dùng khóa này để kiểm tra Tính lũy đẳng (Idempotency), loại bỏ các yêu cầu đặt chỗ lặp lại do lỗi mạng.

#### 4.4. Tính năng Xử lý Hậu kỳ Bất đồng bộ (Async Post-Processing Feature)

- **Tên/Giao thức:** `Kafka Consumer Event Listener` (Giao thức truyền thông điệp, không phải HTTP API).
- **Mô tả:** Một tiến trình công nhân (Worker) chạy ngầm dưới nền, liên tục rút các thông điệp "Đặt slot thành công" ra khỏi hàng đợi để thực thi các nghiệp vụ trễ như: gửi Email xác nhận, đẩy dữ liệu qua hệ thống thanh toán, hoặc ghi nhật ký kiểm toán (Audit Log).
- **Đóng góp chiến lược:**
  - **Phân tán & Song song:** Việc tách rời hoàn toàn các tác vụ nặng ra khỏi luồng giao dịch cốt lõi giúp luồng HTTP của API `POST /api/v1/bookings` trả về kết quả thành công cho người dùng chỉ trong vài mili-giây. Các tiến trình song song trên các cụm máy chủ khác sẽ từ từ xử lý công việc hậu kỳ mà không gây nghẽn (block) ứng dụng chính.

### 5. Phương pháp Kiểm thử Hiệu năng Tối giản & Tập trung vào Mã nguồn (Minimalist & Code-Centric Performance Testing)

#### 5.1. Tối ưu hóa Giới hạn Hệ điều hành (OS Limit Tuning)

- **Định nghĩa:** Mỗi hệ điều hành nhân Linux đều có một cơ chế bảo vệ ngăn chặn một tiến trình mở quá nhiều tệp hoặc kết nối mạng cùng lúc. **File Descriptor (Bộ mô tả tệp)** là một số nguyên (integer) đại diện cho một tệp đang mở hoặc một kết nối mạng (socket) đang mở. Mặc định, Ubuntu giới hạn con số này ở mức 1024, khiến mọi công cụ kiểm thử tải bị sập (lỗi `Too many open files`) trước khi chạm đến giới hạn của backend.
- **Chiến lược:** Loại bỏ nút thắt cổ chai của hệ điều hành trên máy tính chạy kiểm thử bằng cách tăng giới hạn File Descriptor tạm thời lên mức cực đại trong phiên làm việc hiện tại.
- **Thực hành (Copy-paste vào terminal trước khi test):**
  ```bash
  # Nâng giới hạn file descriptors lên 65535 cho phiên terminal hiện tại
  ulimit -n 65535
  ```

#### 5.2. Công cụ Bơm tải Cục bộ Nguyên khối (Monolithic Load Generator)

- **Định nghĩa:** Thay vì dùng JMeter (chạy trên máy ảo Java nặng nề), ta sử dụng **Grafana k6**. k6 là một công cụ mã nguồn mở được viết bằng ngôn ngữ Go, có khả năng sinh ra tải trọng cực lớn từ một máy tính duy nhất nhờ cơ chế **Goroutines** (các luồng thực thi siêu nhẹ do Go runtime quản lý, thay vì do hệ điều hành quản lý), giúp tiết kiệm tối đa RAM và CPU khi giả lập người dùng ảo.
- **Chiến lược:** Không tạo 100.000 kết nối. Chúng ta sẽ cấu hình k6 sinh ra 1.000 **Virtual Users (Người dùng ảo - VUs)**. Mỗi VU sẽ tái sử dụng liên tục một kết nối (HTTP Keep-Alive) để bắn tổng cộng 100.000 yêu cầu vào API càng nhanh càng tốt. Điều này ép luồng đặt slot của ứng dụng phải liên tục xử lý xung đột.

#### 5.3. Kịch bản Kiểm thử k6 (k6 Test Script)

- **Định nghĩa:** Kịch bản k6 được viết bằng JavaScript, định nghĩa chính xác hành vi của các VUs, bao gồm cấu hình tải, tiêu đề HTTP (Headers), và thân dữ liệu (Payload).
- **Chiến lược:** Cung cấp một kịch bản hoàn chỉnh, trong đó mỗi yêu cầu HTTP được tự động gắn một mã UUID (Universally Unique Identifier - Chuỗi định danh duy nhất toàn cầu) để làm Khóa lũy đẳng (Idempotency Key), mô phỏng chính xác các giao dịch độc lập.

**Thực hành: Tạo tệp `flash-sale-test.js` trong thư mục dự án:**

```javascript
import http from "k6/http";
import { check } from "k6";
import { uuidv4 } from "https://jslib.k6.io/k6-utils/1.4.0/index.js";

// 1. Cấu hình bài test (Test Configuration)
export const options = {
  scenarios: {
    flash_sale_spike: {
      executor: "shared-iterations", // Tất cả VUs cùng chia sẻ một tổng số lượng yêu cầu
      vus: 1000, // 1.000 luồng mạng ảo gọi API cùng một lúc
      iterations: 100000, // Tổng cộng bắn 100.000 yêu cầu
      maxDuration: "30s", // Bài test tự động ngắt sau 30 giây nếu máy chủ bị treo
    },
  },
};

// 2. Logic nghiệp vụ của mỗi yêu cầu (Virtual User Logic)
export default function () {
  const url = "http://localhost:8080/api/v1/bookings";

  // Giả lập Payload: Đặt 1 slot cho sự kiện ID 1
  const payload = JSON.stringify({
    eventId: 1,
    quantity: 1,
  });

  const params = {
    headers: {
      "Content-Type": "application/json",
      // Sinh mã ngẫu nhiên cho mỗi request để test tính lũy đẳng và giao dịch độc lập
      "Idempotency-Key": uuidv4(),
      // Giả lập ID người dùng từ 1 đến 100000
      "X-User-Id": Math.floor(Math.random() * 100000) + 1,
    },
  };

  // 3. Thực thi HTTP POST Request
  const response = http.post(url, payload, params);

  // 4. Xác quyết trạng thái (Assertions)
  // Hệ thống tốt phải trả về 200 (Thành công) hoặc 409 (Từ chối do hết slot/trùng khóa)
  // Nếu trả về 500 (Internal Server Error) -> Lỗi xử lý đa luồng hoặc sập Database.
  check(response, {
    "is status 200 or 409": (r) => r.status === 200 || r.status === 409,
    "is NOT status 500": (r) => r.status !== 500,
  });
}
```

**Thực hành: Khởi chạy kiểm thử:**

```bash
# Cài đặt k6 cục bộ trên Ubuntu
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6 -y

# Chạy kịch bản
k6 run flash-sale-test.js
```

#### 5.4. Chốt chặn Kiểm toán Cuối cùng (The Ultimate Data Audit)

- **Định nghĩa:** Đây là bước xác minh tính đúng đắn của toàn bộ mô hình dữ liệu (Data Integrity). Dù ứng dụng chạy nhanh đến đâu, nếu kết quả toán học sai, kiến trúc đó vô giá trị.
- **Chiến lược:** Khi k6 chạy xong (100.000 yêu cầu đã được xử lý), giả sử sự kiện ban đầu được nạp (seed) với `10.000 slot`. Bạn mở kết nối trực tiếp vào PostgreSQL và chạy truy vấn tổng hợp sau. Nếu kết quả trả về đúng `10.000` và bảng `slots` ghi nhận `available_quantity = 0`, mã nguồn của bạn đã xử lý thành công trạng thái tương tranh một cách hoàn hảo mà không cần hạ tầng phân tán khổng lồ.

**Thực hành (SQL Query trên PostgreSQL):**

```sql
-- Kiểm tra tổng số slot thực tế đã được cấp phát thành công
SELECT COUNT(*) as total_successful_bookings
FROM reservations
WHERE event_id = 1 AND status = 'CONFIRMED';
```

### 6. Thiết kế Lược đồ Cơ sở dữ liệu Tối ưu (Optimized Database Schema)

**Database Schema (Lược đồ cơ sở dữ liệu):** Là cấu trúc logic định nghĩa cách dữ liệu được tổ chức, lưu trữ và mối quan hệ giữa các thực thể trong một hệ quản trị cơ sở dữ liệu quan hệ (RDBMS).

Chiến lược cốt lõi ở đây là **Vertical Partitioning (Phân mảnh dọc)**: Tách biệt hoàn toàn dữ liệu có "tần suất đọc cao, tần suất ghi cực thấp" (thông tin sự kiện) ra khỏi dữ liệu có "tần suất đọc-ghi cực cao và liên tục" (biến đếm số lượng slot). Điều này giảm thiểu tối đa hiện tượng **Row-Level Contention (Tranh chấp mức hàng)** — tình trạng nhiều giao dịch cùng cố gắng khóa và cập nhật cùng một hàng vật lý trên đĩa cứng.

Dưới đây là mã DDL (Data Definition Language - Ngôn ngữ định nghĩa dữ liệu) hoàn chỉnh dùng cho PostgreSQL, kèm theo giải thích chiến lược cho từng thành phần.

#### 6.1. Bảng Dữ liệu Tĩnh: `events`

- **Mô tả:** Lưu trữ thông tin nguyên bản của sự kiện. Bảng này gần như chỉ phục vụ thao tác đọc (Read-only) trong suốt quá trình diễn ra Flash Sale.
- **Chiến lược Tối ưu:** Không chứa bất kỳ bộ đếm (counter) nào có tính chất thay đổi liên tục, giúp các truy vấn hiển thị thông tin sự kiện trên ứng dụng khách không bao giờ bị nghẽn (block) bởi các giao dịch đặt slot đang diễn ra.

```sql
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

#### 6.2. Bảng Trạng thái Tương tranh: `slot`

- **Mô tả:** Trái tim của hệ thống kiểm soát tải. Bảng này chỉ chứa biến đếm slot còn lại.
- **Chiến lược Tối ưu:**
  1.  **Optimistic Locking (Khóa lạc quan):** Sử dụng cột `version`. Mỗi lần một luồng cập nhật số lượng thành công, `version` tăng thêm 1. Nếu một luồng khác gửi lệnh UPDATE với `version` cũ, PostgreSQL sẽ từ chối, ngăn chặn ghi đè.
  2.  **Database-Level Constraint (Ràng buộc cấp cơ sở dữ liệu):** Dòng `CHECK (available_quantity >= 0)` là phòng tuyến cuối cùng và tuyệt đối nhất. Dù mã nguồn Java của bạn có bị lỗi thuật toán đa luồng, PostgreSQL cũng sẽ ném ra ngoại lệ (Exception) chặn đứng mọi giao dịch cố tình làm số slot rơi xuống số âm (Overbooking).

```sql
CREATE TABLE slot (
    event_id BIGINT PRIMARY KEY REFERENCES events(id),
    available_quantity INTEGER NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    -- Ràng buộc toán học phần cứng: Không bao giờ được phép âm
    CONSTRAINT chk_positive_slot CHECK (available_quantity >= 0)
);

-- Đánh chỉ mục Index để tăng tốc độ tìm kiếm kho theo sự kiện
-- Index (Chỉ mục): Một cấu trúc dữ liệu (B-Tree) giúp truy xuất các bản ghi nhanh hơn theo một điều kiện cụ thể.
CREATE INDEX idx_slot_event_id ON slot(event_id);
```

#### 6.3. Sổ cái Giao dịch: `reservations`

- **Mô tả:** Bảng lưu vết (Audit log) và trạng thái của từng nỗ lực đặt chỗ. Đây là bảng dạng "Append-only" (Chỉ chèn thêm), rất thân thiện với hiệu suất ghi ổ đĩa.
- **Chiến lược Tối ưu:**
  1.  **Idempotency Key (Khóa lũy đẳng):** Cột `idempotency_key` kết hợp với ràng buộc `UNIQUE` đảm bảo rằng nếu một máy khách gửi trùng lặp hàng chục yêu cầu do lỗi mạng (Retry), cơ sở dữ liệu sẽ tự động từ chối bản ghi thứ 2 trở đi.
  2.  **Index Optimization (Tối ưu chỉ mục):** Đánh chỉ mục riêng cho `user_id` và `event_id` để tăng tốc độ truy vấn lịch sử của người dùng sau khi Flash Sale kết thúc.

```sql
CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL, -- Định danh người dùng lấy từ Token xác thực
    event_id BIGINT NOT NULL REFERENCES events(id),
    quantity INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL, -- Các trạng thái: PENDING (Đang chờ xử lý), CONFIRMED (Thành công), FAILED (Thất bại)
    idempotency_key VARCHAR(255) UNIQUE NOT NULL, -- Khóa chống trùng lặp giao dịch
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Đánh chỉ mục để tăng tốc độ kiểm tra xem người dùng đã đặt sự kiện này chưa (nếu có luật 1 người/1 vé)
CREATE INDEX idx_reservations_user_event ON reservations(user_id, event_id);

-- Đánh chỉ mục cho khóa lũy đẳng để hỗ trợ truy vấn xác minh trạng thái nhanh
CREATE INDEX idx_reservations_idempotency ON reservations(idempotency_key);
```
