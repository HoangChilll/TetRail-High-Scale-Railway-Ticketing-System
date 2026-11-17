-- Chỉ cho phép 1 vé ACTIVE (RESERVED hoặc ISSUED) trên cùng một chuyến và một ghế
CREATE UNIQUE INDEX uk_tickets_active_trip_seat 
ON tickets(trip_id, seat_id) 
WHERE status IN ('RESERVED', 'ISSUED');