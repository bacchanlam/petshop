package com.example.doan_petshop.controller;

import com.example.doan_petshop.dto.SiteNotification;
import com.example.doan_petshop.entity.User;
import com.example.doan_petshop.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService            userService;
    private final SimpMessagingTemplate  messagingTemplate;

    // ========================
    // GET - Danh sách user
    // ========================
    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/users/list";
    }

    // ========================
    // POST - Khóa / Mở khóa tài khoản
    // ========================
    @PostMapping("/toggle/{id}")
    public String toggleEnabled(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        try {
            userService.toggleUserEnabled(id);
            User user = userService.findById(id);
            String type = user.getEnabled() ? "ACCOUNT_UNLOCKED" : "ACCOUNT_LOCKED";
            String msg  = user.getEnabled() ? "Tài khoản của bạn đã được mở khóa."
                                            : "Tài khoản của bạn đã bị khóa bởi quản trị viên.";
            messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/account-status",
                    new SiteNotification(type, id, msg));
            redirectAttributes.addFlashAttribute("successMsg", "Đã cập nhật trạng thái tài khoản.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ========================
    // POST - Phân quyền
    // ========================
    @PostMapping("/role/{id}")
    public String changeRole(@PathVariable Long id,
                             @RequestParam String roleName,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.changeRole(id, roleName);
            User user = userService.findById(id);
            messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/account-status",
                    new SiteNotification("ROLE_CHANGED", id, "Quyền tài khoản của bạn vừa được thay đổi."));
            redirectAttributes.addFlashAttribute("successMsg", "Đã cập nhật quyền tài khoản.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}