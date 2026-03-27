package com.example.doan_petshop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderStatusNotification {
    private Long   orderId;
    private String status;      // e.g. "CONFIRMED"
    private String displayName; // e.g. "Đã xác nhận"
}
