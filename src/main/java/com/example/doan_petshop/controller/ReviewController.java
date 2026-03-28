package com.example.doan_petshop.controller;

import com.example.doan_petshop.dto.AdminNotification;
import com.example.doan_petshop.security.CustomUserDetails;
import com.example.doan_petshop.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService          reviewService;
    private final SimpMessagingTemplate  messagingTemplate;

    // ========================
    // POST /reviews/add - Gửi đánh giá
    // ========================
    @PostMapping("/add")
    public String addReview(@AuthenticationPrincipal CustomUserDetails userDetails,
                            @RequestParam Long    productId,
                            @RequestParam int     rating,
                            @RequestParam(required = false) String comment,
                            RedirectAttributes redirectAttributes) {
        try {
            reviewService.addReview(userDetails.getId(), productId, rating, comment);
            redirectAttributes.addFlashAttribute("successMsg",
                    "Cảm ơn bạn đã đánh giá sản phẩm!");

            // Thông báo real-time cho admin (không truy cập lazy relation sau transaction)
            String stars = "★".repeat(rating) + "☆".repeat(5 - rating);
            messagingTemplate.convertAndSend("/topic/admin-updates",
                    new AdminNotification("NEW_REVIEW", productId,
                            "Đánh giá mới " + stars + " (sản phẩm #" + productId + ")"
                                    + " bởi " + userDetails.getUser().getFullName(),
                            null, null, null, null, null, null, null, null));
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg",
                    "Có lỗi xảy ra khi gửi đánh giá. Vui lòng thử lại.");
        }
        return "redirect:/products/" + productId;
    }
}