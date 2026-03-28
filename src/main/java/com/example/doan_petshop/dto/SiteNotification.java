package com.example.doan_petshop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket payload gửi đến /topic/site-updates (broadcast cho tất cả user)
 * hoặc /user/queue/account-status (cho user cụ thể).
 *
 * type values:
 *   PRODUCT_ADDED, PRODUCT_UPDATED, PRODUCT_ACTIVATED, PRODUCT_DEACTIVATED, PRODUCT_DELETED
 *   CATEGORY_ADDED, CATEGORY_UPDATED, CATEGORY_TOGGLED, CATEGORY_DELETED
 *   ACCOUNT_LOCKED, ACCOUNT_UNLOCKED, ROLE_CHANGED
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SiteNotification {
    private String type;
    private Long   entityId;   // productId / categoryId (null nếu không liên quan)
    private String message;    // Nội dung hiển thị cho user
}
