package com.example.doan_petshop.repository;

import com.example.doan_petshop.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT COUNT(i) FROM OrderItem i WHERE i.order.id = :orderId")
    int countByOrderId(@Param("orderId") Long orderId);
}