package com.example.doan_petshop.controller;

import com.example.doan_petshop.dto.OrderStatusNotification;
import com.example.doan_petshop.entity.Order;
import com.example.doan_petshop.enums.OrderStatus;
import com.example.doan_petshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService           orderService;
    private final SimpMessagingTemplate  messagingTemplate;

    // ========================
    // GET - Danh sách đơn hàng
    // ========================
    @GetMapping
    public String list(@RequestParam(required = false) OrderStatus status,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "0")  int page,
                       @RequestParam(defaultValue = "15") int size,
                       Model model) {

        Page<Order> orders = orderService.findByAdminFilters(status, keyword, page, size);

        model.addAttribute("orders",      orders);
        model.addAttribute("statuses",    OrderStatus.values());
        model.addAttribute("selStatus",   status);
        model.addAttribute("keyword",     keyword);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages",  orders.getTotalPages());

        // Thống kê nhanh theo trạng thái
        model.addAttribute("cntPending",   orderService.countByStatus(OrderStatus.PENDING));
        model.addAttribute("cntConfirmed", orderService.countByStatus(OrderStatus.CONFIRMED));
        model.addAttribute("cntShipping",  orderService.countByStatus(OrderStatus.SHIPPING));
        model.addAttribute("cntCompleted", orderService.countByStatus(OrderStatus.COMPLETED));
        model.addAttribute("cntCancelled", orderService.countByStatus(OrderStatus.CANCELLED));

        return "admin/orders/list";
    }

    // ========================
    // GET - Chi tiết đơn hàng
    // ========================
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Order order = orderService.findById(id);
        model.addAttribute("order",    order);
        model.addAttribute("statuses", OrderStatus.values());
        return "admin/orders/detail";
    }

    // ========================
    // POST - Cập nhật trạng thái
    // ========================
    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam OrderStatus status,
                               RedirectAttributes redirectAttributes) {
        try {
            orderService.updateStatus(id, status);

            // Push real-time notification đến user sở hữu đơn hàng
            Order order = orderService.findById(id);
            String username = order.getUser().getUsername();
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/order-status",
                    new OrderStatusNotification(id, status.name(), status.getDisplayName())
            );

            redirectAttributes.addFlashAttribute("successMsg",
                    "Cập nhật trạng thái đơn #" + id + " thành công!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }
}