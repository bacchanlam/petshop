package com.example.doan_petshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Lưu lại tên & ảnh sản phẩm tại thời điểm mua
    // (phòng trường hợp sản phẩm bị sửa hoặc xóa sau này)
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "product_img", length = 255)
    private String productImg;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    // ========================
    // Quan hệ Many-to-One với Order
    // ========================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // ========================
    // Quan hệ Many-to-One với Product (nullable - sản phẩm có thể bị xóa)
    // ========================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    // ========================
    // Helper methods
    // ========================
    public BigDecimal getSubTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}

