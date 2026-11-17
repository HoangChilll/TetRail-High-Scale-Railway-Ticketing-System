-- ==============================================================================
-- Migration V5: Pre-allocated Ticket Inventory Pattern
-- ==============================================================================

-- 1. Cho phép booking_id và passenger_name nhận NULL khi vé được tạo sẵn trong kho
ALTER TABLE tickets ALTER COLUMN booking_id DROP NOT NULL;
ALTER TABLE tickets ALTER COLUMN passenger_name DROP NOT NULL;

-- 2. Đổi default status của bảng vé thành AVAILABLE
ALTER TABLE tickets ALTER COLUMN status SET DEFAULT 'AVAILABLE';

-- 3. Xóa index filter cũ ở V4 (nếu có)
DROP INDEX IF EXISTS uk_tickets_active_trip_seat;

-- 4. Tạo ràng buộc UNIQUE: Trên 1 chuyến tàu (trip), mỗi ghế (seat) chỉ có duy nhất 1 vé tồn tại vĩnh viễn
ALTER TABLE tickets 
    ADD CONSTRAINT uk_tickets_trip_seat UNIQUE (trip_id, seat_id);

