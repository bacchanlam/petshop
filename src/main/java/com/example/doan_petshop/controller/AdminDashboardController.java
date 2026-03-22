package com.example.doan_petshop.controller;

import com.example.doan_petshop.enums.OrderStatus;
import com.example.doan_petshop.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final OrderService    orderService;
    private final ProductService  productService;
    private final UserService     userService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        // ======= STATS CARDS =======
        long cntPending   = orderService.countByStatus(OrderStatus.PENDING);
        long cntConfirmed = orderService.countByStatus(OrderStatus.CONFIRMED);
        long cntShipping  = orderService.countByStatus(OrderStatus.SHIPPING);
        long cntCompleted = orderService.countByStatus(OrderStatus.COMPLETED);
        long cntCancelled = orderService.countByStatus(OrderStatus.CANCELLED);

        model.addAttribute("totalOrders",   cntPending + cntConfirmed + cntShipping + cntCompleted + cntCancelled);
        model.addAttribute("pendingOrders", cntPending);
        model.addAttribute("revenueMonth",  orderService.revenueThisMonth());
        model.addAttribute("ordersToday",   orderService.countToday());
        model.addAttribute("totalProducts", productService.countActive());
        model.addAttribute("totalUsers",    userService.findAll().size());

        // ======= ĐƠN HÀNG THEO TRẠNG THÁI (Biểu đồ doughnut) =======
        model.addAttribute("cntPending",   cntPending);
        model.addAttribute("cntConfirmed", cntConfirmed);
        model.addAttribute("cntShipping",  cntShipping);
        model.addAttribute("cntCompleted", cntCompleted);
        model.addAttribute("cntCancelled", cntCancelled);

        // ======= SẢN PHẨM SẮP HẾT HÀNG =======
        model.addAttribute("lowStockProducts", productService.findLowStock(10));

        // ======= ĐƠN HÀNG CHỜ XỬ LÝ (10 đơn mới nhất) =======
        model.addAttribute("recentOrders",
                orderService.findByAdminFilters(null, null, 0, 10).getContent());

        return "admin/dashboard";
    }
}