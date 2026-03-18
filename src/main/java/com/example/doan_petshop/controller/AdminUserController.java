package com.example.doan_petshop.controller;

import com.example.doan_petshop.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

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
            redirectAttributes.addFlashAttribute("successMsg",
                    "Đã cập nhật trạng thái tài khoản.");
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
            redirectAttributes.addFlashAttribute("successMsg",
                    "Đã cập nhật quyền tài khoản.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}