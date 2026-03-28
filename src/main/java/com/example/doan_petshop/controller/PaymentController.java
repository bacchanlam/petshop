package com.example.doan_petshop.controller;

import com.example.doan_petshop.dto.AdminNotification;
import com.example.doan_petshop.entity.Order;
import com.example.doan_petshop.entity.Cart;
import com.example.doan_petshop.enums.OrderStatus;
import com.example.doan_petshop.enums.PaymentMethod;
import com.example.doan_petshop.security.CustomUserDetails;
import com.example.doan_petshop.service.CartService;
import com.example.doan_petshop.service.MomoService;
import com.example.doan_petshop.service.OrderService;
import com.example.doan_petshop.dto.OrderRequestDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final MomoService            momoService;
    private final OrderService           orderService;
    private final CartService            cartService;
    private final SimpMessagingTemplate  messagingTemplate;

    // =========================================================
    // POST /payment/momo/create
    // Tạo đơn hàng DB rồi redirect sang MoMo
    // =========================================================
    @PostMapping("/momo/create")
    public String createMomoPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute OrderRequestDTO dto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Xác thực dữ liệu đầu vào
        if (bindingResult.hasErrors()) {
            // Lấy cart để hiển thị lại trang form
            Cart cart = cartService.getCart(userDetails.getId());

            // Tính phí ship server-side
            BigDecimal subtotal = cart.getTotalPrice();
            BigDecimal shippingFee = subtotal.compareTo(new BigDecimal("300000")) >= 0
                    ? BigDecimal.ZERO : new BigDecimal("30000");
            BigDecimal grandTotal = subtotal.add(shippingFee);

            model.addAttribute("cart", cart);
            model.addAttribute("orderRequestDTO", dto);
            model.addAttribute("paymentMethods", PaymentMethod.values());
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("shippingFee", shippingFee);
            model.addAttribute("grandTotal", grandTotal);
            model.addAttribute("errorMsg", "Vui lòng kiểm tra thông tin nhập vào.");

            return "user/checkout";
        }

        try {
            dto.setPaymentMethod(PaymentMethod.MOMO);
            Order order = orderService.placeOrder(userDetails.getId(), dto);

            // Gọi MoMo API tạo payment URL
            String orderInfo = "Thanh toan don hang #" + order.getId() + " - PetShop";
            long amount = order.getTotalAmount().longValue();

            MomoService.MomoPaymentResponse momoResp =
                    momoService.createPayment(order.getId(), amount, orderInfo);

            if (momoResp.isSuccess()) {
                log.info("[MoMo] Payment created for order #{}, payUrl: {}", order.getId(), momoResp.payUrl());
                // 3. Redirect sang MoMo để user thanh toán
                return "redirect:" + momoResp.payUrl();
            } else {
                // MoMo lỗi, hủy đơn, quay về checkout
                log.error("[MoMo] Create payment failed: {} - {}", momoResp.resultCode(), momoResp.message());
                orderService.cancelOrder(order.getId(), userDetails.getId());
                redirectAttributes.addFlashAttribute("errorMsg",
                        "Không thể tạo thanh toán MoMo: " + momoResp.message());
                return "redirect:/orders/checkout";
            }

        } catch (Exception e) {
            log.error("[MoMo] Exception Stack:", e);
            log.error("[MoMo] Error creating payment - Message: {}", e.getMessage());
            log.error("[MoMo] Error creating payment - Cause: {}", e.getCause());
            redirectAttributes.addFlashAttribute("errorMsg",
                    "Có lỗi xảy ra khi tạo thanh toán MoMo: " + e.getMessage());
            return "redirect:/orders/checkout";
        }
    }

    // =========================================================
    // GET /payment/momo/return
    // MoMo redirect user về app sau khi thanh toán
    // =========================================================
    @GetMapping("/momo/return")
    public String momoReturn(
            @RequestParam Map<String, String> params,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes,
            Model model) {

        log.info("[MoMo] Return params: {}", params);

        String resultCode = params.getOrDefault("resultCode", "-1");
        String extraData  = params.getOrDefault("extraData", "");
        String message    = params.getOrDefault("message", "");

        Long orderId = momoService.extractOrderId(extraData);

        // resultCode = 0 ,thành công
        if ("0".equals(resultCode)) {
            if (orderId != null) {
                try {
                    // Xác nhận đơn hàng tự động
                    orderService.confirmPayment(orderId, PaymentMethod.MOMO);
                    log.info("[MoMo] Order #{} confirmed after payment", orderId);
                    // Thông báo real-time cho admin
                    sendNewOrderNotification(orderId);
                } catch (Exception e) {
                    log.error("[MoMo] Error confirming order #{}", orderId, e);
                }
            }
            redirectAttributes.addFlashAttribute("orderId", orderId);
            redirectAttributes.addFlashAttribute("paymentMethod", "MOMO");
            return "redirect:/orders/success";
        } else {
            // Thanh toán thất bại hoặc user hủy
            log.warn("[MoMo] Payment failed/cancelled. resultCode={}, message={}", resultCode, message);
            if (orderId != null) {
                try {
                    // Hủy đơn hàng nếu thanh toán thất bại
                    orderService.cancelOrderByAdmin(orderId);
                } catch (Exception e) {
                    log.warn("[MoMo] Could not cancel order #{}", orderId);
                }
            }
            redirectAttributes.addFlashAttribute("errorMsg",
                    "Thanh toán MoMo không thành công: " + message + " (Mã: " + resultCode + ")");
            return "redirect:/orders/checkout";
        }
    }

    // =========================================================
    // POST /payment/momo/ipn
    // MoMo gọi IPN (server-to-server) để xác nhận thanh toán
    // =========================================================
    @PostMapping("/momo/ipn")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> momoIpn(
            @RequestBody Map<String, String> params,
            HttpServletRequest request) {

        log.info("[MoMo IPN] Received: {}", params);

        Map<String, Object> response = new HashMap<>();

        // Xác thực chữ ký từ MoMo
        if (!momoService.verifyIpnSignature(params)) {
            log.warn("[MoMo IPN] Invalid signature!");
            response.put("resultCode", 1);
            response.put("message", "Invalid signature");
            return ResponseEntity.ok(response);
        }

        String resultCode = params.getOrDefault("resultCode", "-1");
        String extraData  = params.getOrDefault("extraData", "");

        if ("0".equals(resultCode)) {
            Long orderId = momoService.extractOrderId(extraData);
            if (orderId != null) {
                try {
                    orderService.confirmPayment(orderId, PaymentMethod.MOMO);
                    log.info("[MoMo IPN] Order #{} confirmed via IPN", orderId);
                    // Thông báo real-time cho admin (IPN đến trước return)
                    sendNewOrderNotification(orderId);
                } catch (Exception e) {
                    log.error("[MoMo IPN] Error confirming order #{}", orderId, e);
                }
            }
            response.put("resultCode", 0);
            response.put("message", "success");
        } else {
            response.put("resultCode", 0);
            response.put("message", "payment failed, order not confirmed");
        }

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // Helper: gửi WebSocket notification cho admin
    // =========================================================
    private void sendNewOrderNotification(Long orderId) {
        try {
            Order order = orderService.findById(orderId);
            String amountStr = NumberFormat.getNumberInstance(new Locale("vi", "VN"))
                    .format(order.getTotalAmount()) + " ₫";
            LocalDateTime createdAt = order.getCreatedAt() != null
                    ? order.getCreatedAt() : LocalDateTime.now();
            String dateStr = createdAt.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));

            int itemCount = orderService.countItems(order.getId());
            AdminNotification notif = new AdminNotification(
                    "NEW_ORDER", order.getId(),
                    "Đơn hàng mới #" + order.getId() + " từ " + order.getFullName()
                            + " – " + amountStr + " [MoMo]",
                    order.getFullName(),
                    order.getPhone(),
                    itemCount,
                    order.getTotalAmount().longValue(),
                    order.getPaymentMethod().getDisplayName(),
                    order.getStatus().name(),
                    order.getStatus().getDisplayName(),
                    dateStr
            );
            messagingTemplate.convertAndSend("/topic/admin-updates", notif);
            log.info("[MoMo] Admin notified for order #{}", orderId);
        } catch (Exception e) {
            log.warn("[MoMo] Could not send admin notification for order #{}: {}", orderId, e.getMessage());
        }
    }
}