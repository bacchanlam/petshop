package com.example.doan_petshop.controller;

import com.example.doan_petshop.entity.Cart;
import com.example.doan_petshop.security.CustomUserDetails;
import com.example.doan_petshop.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // ========================
    // GET /cart - Xem giỏ hàng
    // ========================
    @GetMapping
    public String viewCart(@AuthenticationPrincipal CustomUserDetails userDetails,
                           Model model) {
        Cart cart = cartService.getCart(userDetails.getId());
        model.addAttribute("cart", cart);
        return "user/cart";
    }

    // ========================
    // POST /cart/add - Thêm vào giỏ
    // ========================
    @PostMapping("/add")
    public String addToCart(@AuthenticationPrincipal CustomUserDetails userDetails,
                            @RequestParam Long productId,
                            @RequestParam(defaultValue = "1") int quantity,
                            @RequestParam(required = false) String redirect,
                            RedirectAttributes redirectAttributes) {
        try {
            cartService.addToCart(userDetails.getId(), productId, quantity);
            redirectAttributes.addFlashAttribute("successMsg", "Đã thêm sản phẩm vào giỏ hàng!");
        } catch ( RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }

        // Quay về trang trước hoặc trang giỏ hàng
        if (redirect != null && !redirect.isBlank()) {
            return "redirect:" + redirect;
        }
        return "redirect:/cart";
    }

    // ========================
    // POST /cart/buy-now - Mua ngay → thêm giỏ rồi chuyển checkout
    // ========================
    @PostMapping("/buy-now")
    public String buyNow(@AuthenticationPrincipal CustomUserDetails userDetails,
                         @RequestParam Long productId,
                         @RequestParam(defaultValue = "1") int quantity,
                         RedirectAttributes redirectAttributes) {
        try {
            cartService.addToCart(userDetails.getId(), productId, quantity);
        } catch ( RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/products/" + productId;
        }
        return "redirect:/orders/checkout";
    }

    // ========================
    // POST /cart/update - Cập nhật số lượng
    // ========================
    @PostMapping("/update")
    public String updateCart(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @RequestParam Long itemId,
                             @RequestParam int quantity,
                             RedirectAttributes redirectAttributes) {
        try {
            cartService.updateQuantity(userDetails.getId(), itemId, quantity);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/cart";
    }

    // ========================
    // POST /cart/remove - Xóa 1 sản phẩm
    // ========================
    @PostMapping("/remove")
    public String removeItem(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @RequestParam Long itemId,
                             RedirectAttributes redirectAttributes) {
        try {
            cartService.removeItem(userDetails.getId(), itemId);
            redirectAttributes.addFlashAttribute("successMsg", "Đã xóa sản phẩm khỏi giỏ hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/cart";
    }

    // ========================
    // POST /cart/clear - Xóa toàn bộ giỏ
    // ========================
    @PostMapping("/clear")
    public String clearCart(@AuthenticationPrincipal CustomUserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        cartService.clearCart(userDetails.getId());
        redirectAttributes.addFlashAttribute("successMsg", "Đã xóa toàn bộ giỏ hàng.");
        return "redirect:/cart";
    }

    // ========================
    // GET /cart/count - API đếm số lượng (dùng cho navbar badge)
    // ========================
    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<Map<String, Integer>> countItems(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(Map.of("count", 0));
        }
        int count = cartService.countItems(userDetails.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }
}