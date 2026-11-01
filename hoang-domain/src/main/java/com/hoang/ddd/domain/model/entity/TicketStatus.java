package com.hoang.ddd.domain.model.entity;

public enum TicketStatus {
    AVAILABLE, // Vé đang mở bán trong kho (chưa có ai đặt)
    RESERVED, // Đang được giữ chỗ (chờ thanh toán 10 phút)
    ISSUED, // Đã thanh toán thành công (vé chính thức)
    CANCELLED // Đã hủy / không sử dụng
}