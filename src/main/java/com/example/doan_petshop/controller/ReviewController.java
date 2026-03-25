package com.example.doan_petshop.controller;

import com.example.doan_petshop.security.CustomUserDetails;
import com.example.doan_petshop.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

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
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/products/" + productId;
    }
}