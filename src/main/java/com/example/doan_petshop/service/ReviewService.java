package com.example.doan_petshop.service;

import com.example.doan_petshop.entity.*;
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

    // ========================
    // THÊM đánh giá
    // ========================
    @Transactional
    public Review addReview(Long userId, Long productId, int rating, String comment) {
        // Lấy đơn hàng eligible (đã COMPLETED, chưa review)
        List<Long> eligibleOrderIds = reviewRepository.findEligibleOrderIds(userId, productId);
        if (eligibleOrderIds.isEmpty()) {
            throw new IllegalStateException(
                    "Bạn cần mua và nhận hàng thành công mới có thể đánh giá sản phẩm này"
            );
        }

        // Lấy đơn hàng đầu tiên eligible
        Long orderId = eligibleOrderIds.get(0);

        User    user    = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
        Order   order   = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        Review review = Review.builder()
                .user(user)
                .product(product)
                .order(order)
                .rating(rating)
                .comment(comment)
                .build();

        return reviewRepository.save(review);
    }

    // Kiểm tra user có thể review không
    public boolean canReview(Long userId, Long productId) {
        return !reviewRepository.findEligibleOrderIds(userId, productId).isEmpty();
    }

    // Lấy review theo sản phẩm
    public List<Review> findByProduct(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }
}