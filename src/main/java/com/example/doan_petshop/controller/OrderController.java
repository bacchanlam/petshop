package com.example.doan_petshop.controller;

import com.example.doan_petshop.dto.AdminNotification;
import com.example.doan_petshop.dto.OrderRequestDTO;
import com.example.doan_petshop.entity.Cart;
import com.example.doan_petshop.entity.Order;
import com.example.doan_petshop.enums.PaymentMethod;
import com.example.doan_petshop.security.CustomUserDetails;
import com.example.doan_petshop.service.CartService;
import com.example.doan_petshop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService           orderService;
    private final CartService            cartService;
    private final SimpMessagingTemplate  messagingTemplate;

    // ========================
    // GET /orders/checkout - Trang đặt hàng
    // ========================
    @GetMapping("/checkout")
    public String checkoutPage(@AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model) {
        Cart cart = cartService.getCart(userDetails.getId());

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        // Tính phí ship server-side
        BigDecimal subtotal = cart.getTotalPrice();
        BigDecimal shippingFee = subtotal.compareTo(new BigDecimal("300000")) >= 0
                ? BigDecimal.ZERO : new BigDecimal("30000");
        BigDecimal grandTotal = subtotal.add(shippingFee);

        OrderRequestDTO dto = new OrderRequestDTO();
        dto.setFullName(userDetails.getUser().getFullName());
        dto.setPhone(userDetails.getUser().getPhone());
        dto.setAddress(userDetails.getUser().getAddress());
        dto.setPaymentMethod(PaymentMethod.COD);

        model.addAttribute("cart",           cart);
        model.addAttribute("orderRequestDTO", dto);
        model.addAttribute("paymentMethods",  PaymentMethod.values());
        model.addAttribute("subtotal",        subtotal);
        model.addAttribute("shippingFee",     shippingFee);
        model.addAttribute("grandTotal",      grandTotal);
        return "user/checkout";
    }

    // ========================
    // POST /orders/checkout - Xử lý đặt hàng
    // ========================
    @PostMapping("/checkout")
    public String placeOrder(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @Valid @ModelAttribute("orderRequestDTO") OrderRequestDTO dto,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            Cart cart = cartService.getCart(userDetails.getId());
            model.addAttribute("cart",          cart);
            model.addAttribute("paymentMethods", PaymentMethod.values());
            return "user/checkout";
        }

        try {
            Order order = orderService.placeOrder(userDetails.getId(), dto);
            redirectAttributes.addFlashAttribute("orderId", order.getId());
            redirectAttributes.addFlashAttribute("paymentMethod", order.getPaymentMethod().name());
            redirectAttributes.addFlashAttribute("totalAmount", order.getTotalAmount().longValue());

            // Thông báo real-time cho admin
            String amountStr = NumberFormat.getNumberInstance(new Locale("vi", "VN"))
                    .format(order.getTotalAmount()) + " ₫";
            LocalDateTime createdAt = order.getCreatedAt() != null
                    ? order.getCreatedAt() : LocalDateTime.now();
            String dateStr = createdAt.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
            AdminNotification notif = new AdminNotification(
                    "NEW_ORDER", order.getId(),
                    "Đơn hàng mới #" + order.getId() + " từ " + order.getFullName() + " – " + amountStr,
                    order.getFullName(),
                    order.getPhone(),
                    order.getOrderItems().size(),
                    order.getTotalAmount().longValue(),
                    order.getPaymentMethod().getDisplayName(),
                    order.getStatus().name(),
                    order.getStatus().getDisplayName(),
                    dateStr
            );
            messagingTemplate.convertAndSend("/topic/admin-updates", notif);

            return "redirect:/orders/success";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMsg", e.getMessage());
            Cart cart = cartService.getCart(userDetails.getId());
            model.addAttribute("cart",          cart);
            model.addAttribute("paymentMethods", PaymentMethod.values());
            return "user/checkout";
        }
    }

    // ========================
    // GET /orders/success - Đặt hàng thành công
    // ========================
    @GetMapping("/success")
    public String orderSuccess(@AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model) {
        // orderId được truyền qua flashAttribute
        return "user/order-success";
    }

    // ========================
    // GET /orders/history - Lịch sử đơn hàng
    // ========================
    @GetMapping("/history")
    public String orderHistory(@AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model) {
        model.addAttribute("orders",
                orderService.getOrderHistory(userDetails.getId()));
        return "user/order-history";
    }

    // ========================
    // GET /orders/{id} - Chi tiết đơn hàng
    // ========================
    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              Model model) {
        try {
            Order order = orderService.getOrderDetail(id, userDetails.getId(), false);
            model.addAttribute("order", order);
            return "user/order-detail";
        } catch (SecurityException e) {
            return "redirect:/orders/history";
        }
    }

    // ========================
    // POST /orders/{id}/cancel - Hủy đơn hàng
    // ========================
    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        try {
            orderService.cancelOrder(id, userDetails.getId());
            redirectAttributes.addFlashAttribute("successMsg", "Đã hủy đơn hàng thành công.");

            // Thông báo real-time cho admin
            messagingTemplate.convertAndSend("/topic/admin-updates",
                    new AdminNotification("ORDER_CANCELLED", id,
                            "Đơn hàng #" + id + " bị hủy bởi khách "
                                    + userDetails.getUser().getFullName(),
                            null, null, null, null, null, "CANCELLED", "Đã hủy", null));
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/orders/" + id;
    }
}