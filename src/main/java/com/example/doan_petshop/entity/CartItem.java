package com.example.doan_petshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id", "product_id"})
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    // ========================
    // Quan hệ Many-to-One với Cart
    // ========================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    // ========================
    // Quan hệ Many-to-One với Product
    // ========================
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // ========================
    // Helper methods
    // ========================
    public BigDecimal getSubTotal() {
        return product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }
}

