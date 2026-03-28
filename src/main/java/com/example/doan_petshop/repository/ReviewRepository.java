package com.example.doan_petshop.repository;

import com.example.doan_petshop.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Kiểm tra user đã review sản phẩm này của đơn hàng đó chưa
    boolean existsByUserIdAndProductIdAndOrderId(Long userId, Long productId, Long orderId);

    // Kiểm tra user đã từng review sản phẩm này chưa (bất kể đơn hàng nào)
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    // Lấy review theo sản phẩm
    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

    // Tính điểm trung bình của sản phẩm
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double avgRatingByProductId(@Param("productId") Long productId);

    // Lấy đơn hàng COMPLETED của user có chứa sản phẩm (để chọn orderId khi review)
    @Query("""
        SELECT DISTINCT o.id FROM Order o
        JOIN o.orderItems oi
        WHERE o.user.id     = :userId
          AND oi.product.id  = :productId
          AND o.status       = 'COMPLETED'
          AND o.id NOT IN (
              SELECT r.order.id FROM Review r
              WHERE r.user.id = :userId AND r.product.id = :productId
          )
    """)
    List<Long> findEligibleOrderIds(
            @Param("userId")    Long userId,
            @Param("productId") Long productId
    );
}