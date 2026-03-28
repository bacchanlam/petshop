package com.example.doan_petshop.repository;

import com.example.doan_petshop.entity.Order;
import com.example.doan_petshop.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Lịch sử đơn hàng của user (mới nhất trước)
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Admin - lấy tất cả đơn có lọc + phân trang
    @Query("""
        SELECT o FROM Order o
        WHERE (:status IS NULL OR o.status = :status)
          AND (:keyword IS NULL
               OR LOWER(o.fullName) LIKE LOWER(CONCAT('%',:keyword,'%'))
               OR LOWER(o.phone)    LIKE LOWER(CONCAT('%',:keyword,'%')))
        ORDER BY o.createdAt DESC
    """)
    Page<Order> findByAdminFilters(
            @Param("status")  OrderStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // Kiểm tra user đã mua & nhận sản phẩm chưa (để cho phép review)
    @Query("""
        SELECT COUNT(o) > 0 FROM Order o
        JOIN o.orderItems oi
        WHERE o.user.id    = :userId
          AND oi.product.id = :productId
          AND o.status      = 'COMPLETED'
    """)
    boolean hasUserPurchasedProduct(
            @Param("userId")    Long userId,
            @Param("productId") Long productId
    );

    // Dashboard: đếm đơn hàng theo ngày
    @Query("""
        SELECT COUNT(o) FROM Order o
        WHERE o.createdAt >= :from AND o.createdAt <= :to
    """)
    long countByDateRange(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to
    );

    // Dashboard: doanh thu theo tháng (chỉ tính đơn COMPLETED)
    @Query("""
        SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o
        WHERE o.status = 'COMPLETED'
          AND o.createdAt >= :from AND o.createdAt <= :to
    """)
    BigDecimal sumRevenueByDateRange(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to
    );

    // Đếm đơn theo trạng thái (cho dashboard)
    long countByStatus(OrderStatus status);
    boolean existsByUserIdAndStatusAndOrderItems_Product_Id(
            Long userId, OrderStatus status, Long productId
    );

    // Lấy đơn COMPLETED của user có chứa sản phẩm (để gán vào review)
    @Query("""
        SELECT DISTINCT o FROM Order o
        JOIN o.orderItems oi
        WHERE o.user.id    = :userId
          AND oi.product.id = :productId
          AND o.status      = 'COMPLETED'
        ORDER BY o.createdAt DESC
    """)
    List<Order> findCompletedOrdersByUserAndProduct(
            @Param("userId")    Long userId,
            @Param("productId") Long productId
    );
}