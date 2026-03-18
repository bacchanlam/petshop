package com.example.doan_petshop.controller;

import com.example.doan_petshop.dto.UserRegisterDTO;
import com.example.doan_petshop.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    // ========================
    // GET /auth/login
    // ========================
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error",  required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        if (error != null) {
            model.addAttribute("errorMsg", "Tên đăng nhập hoặc mật khẩu không đúng!");
        }
        if (logout != null) {
            model.addAttribute("successMsg", "Bạn đã đăng xuất thành công.");
        }
        return "auth/login";
    }

    // ========================
    // GET /auth/register
    // ========================
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerDTO", new UserRegisterDTO());
        return "auth/register";
    }

    // ========================
    // POST /auth/register
    // ========================
    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerDTO") UserRegisterDTO dto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Lỗi validation từ annotation
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        // Kiểm tra thủ công (username, email trùng, pass không khớp)
        try {
            userService.register(dto);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            return "auth/register";
        }

        redirectAttributes.addFlashAttribute("successMsg",
                "Đăng ký thành công! Vui lòng đăng nhập.");
        return "redirect:/auth/login";
    }
}