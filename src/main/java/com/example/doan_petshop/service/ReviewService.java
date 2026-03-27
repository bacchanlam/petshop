package com.example.doan_petshop.service;

import com.example.doan_petshop.entity.*;
import com.example.doan_petshop.enums.OrderStatus;
import com.example.doan_petshop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository   reviewRepository;
    private final OrderRepository    orderRepository;
    private final ProductRepository  productRepository;
    private final UserRepository     userRepository;

    @Transactional
    public Review addReview(Long userId, Long productId, int rating, String comment) {
        List<Order> completedOrders = orderRepository.findCompletedOrdersByUserAndProduct(
                userId, productId
        );

        if (completedOrders.isEmpty()) {
            throw new IllegalStateException(
                    "Bạn cần mua và nhận hàng thành công mới có thể đánh giá sản phẩm này."
            );
        }

        User    user    = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        Review review = Review.builder()
                .user(user)
                .product(product)
                .order(completedOrders.get(0))
                .rating(rating)
                .comment(comment)
                .build();

        return reviewRepository.save(review);
    }

    // Kiểm tra user có thể review không (đã mua hàng thành công)
    public boolean canReview(Long userId, Long productId) {
        return orderRepository.existsByUserIdAndStatusAndOrderItems_Product_Id(
                userId, OrderStatus.COMPLETED, productId
        );
    }

    // Lấy review theo sản phẩm
    public List<Review> findByProduct(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }
}