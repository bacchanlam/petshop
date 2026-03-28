package com.example.doan_petshop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket payload gửi đến /topic/admin-updates
 * Dùng để thông báo real-time cho admin khi user thực hiện hành động.
 *
 * type values:
 *   NEW_ORDER       – Khách vừa đặt đơn hàng mới
 *   ORDER_CANCELLED – Khách vừa hủy đơn hàng
 *   NEW_REVIEW      – Khách vừa gửi đánh giá sản phẩm
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminNotification {
    private String  type;                 // NEW_ORDER | ORDER_CANCELLED | NEW_REVIEW
    private Long    entityId;             // orderId hoặc productId
    private String  message;              // Nội dung hiển thị (toast)

    // --- Dữ liệu bổ sung để render row trực tiếp (chỉ dùng cho đơn hàng) ---
    private String  customerName;         // Họ tên khách
    private String  customerPhone;        // SĐT khách
    private Integer itemCount;            // Số sản phẩm
    private Long    totalAmount;          // Tổng tiền (VND, dạng số)
    private String  paymentMethodDisplay; // Tên phương thức thanh toán (hiển thị)
    private String  statusKey;            // Tên enum: PENDING, CONFIRMED, CANCELLED...
    private String  statusDisplay;        // Tên trạng thái tiếng Việt
    private String  dateStr;              // Ngày giờ đặt hàng (dd/MM HH:mm)
}
