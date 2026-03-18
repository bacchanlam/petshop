package com.example.doan_petshop.entity;

import com.example.doan_petshop.enums.PetType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Min(0)
    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "pet_type", nullable = false)
    @Builder.Default
    private PetType petType = PetType.OTHER;

    @Column(length = 100)
    private String brand;

    @Column(length = 255)
    private String thumbnail;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    // ========================
    // Field tạm — không lưu vào DB
    // Dùng để hiển thị số lượng đã bán trên trang chủ
    // ========================
    @Transient
    @Builder.Default
    private Integer soldCount = 0;

    // ========================
    // Helper methods
    // ========================
    public boolean isInStock() {
        return this.stock != null && this.stock > 0;
    }

    public double getAverageRating() {
        if (reviews == null || reviews.isEmpty()) return 0;
        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0);
    }
}