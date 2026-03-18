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
        model.addAttribute("totalOrders",    orderService.countByStatus(OrderStatus.PENDING)
                + orderService.countByStatus(OrderStatus.CONFIRMED)
                + orderService.countByStatus(OrderStatus.SHIPPING)
                + orderService.countByStatus(OrderStatus.COMPLETED)
                + orderService.countByStatus(OrderStatus.CANCELLED));
        model.addAttribute("pendingOrders",  orderService.countByStatus(OrderStatus.PENDING));
        model.addAttribute("revenueMonth",   orderService.revenueThisMonth());
        model.addAttribute("ordersToday",    orderService.countToday());
        model.addAttribute("totalProducts",  productService.countActive());
        model.addAttribute("totalUsers",     userService.findAll().size());

        // ======= ĐƠN HÀNG THEO TRẠNG THÁI (Biểu đồ doughnut) =======
        model.addAttribute("cntPending",   orderService.countByStatus(OrderStatus.PENDING));
        model.addAttribute("cntConfirmed", orderService.countByStatus(OrderStatus.CONFIRMED));
        model.addAttribute("cntShipping",  orderService.countByStatus(OrderStatus.SHIPPING));
        model.addAttribute("cntCompleted", orderService.countByStatus(OrderStatus.COMPLETED));
        model.addAttribute("cntCancelled", orderService.countByStatus(OrderStatus.CANCELLED));

        // ======= SẢN PHẨM SẮP HẾT HÀNG =======
        model.addAttribute("lowStockProducts", productService.findLowStock(10));

        // ======= ĐƠN HÀNG CHỜ XỬ LÝ (10 đơn mới nhất) =======
        model.addAttribute("recentOrders",
                orderService.findByAdminFilters(null, null, 0, 10).getContent());

        return "admin/dashboard";
    }
}