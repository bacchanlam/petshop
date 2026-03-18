package com.example.doan_petshop.repository;

import com.example.doan_petshop.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Tìm item trong giỏ theo cartId + productId (tránh thêm trùng)
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    // Xóa toàn bộ item của một giỏ (dùng sau khi đặt hàng)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteByCartId(@Param("cartId") Long cartId);
}