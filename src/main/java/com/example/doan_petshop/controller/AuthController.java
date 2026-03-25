package com.example.doan_petshop.controller;

import com.example.doan_petshop.dto.UserRegisterDTO;
import com.example.doan_petshop.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    // ========================
    // GET /auth/login
    // ========================
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error",  required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model,
            HttpSession session) {

        // Lấy errorMsg từ session (do CustomAuthenticationFailureHandler set)
        Object sessionError = session.getAttribute("errorMsg");
        if (sessionError != null) {
            model.addAttribute("errorMsg", sessionError);
            session.removeAttribute("errorMsg");
        } else if (error != null) {
            // Fallback nếu không có session error
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
            var savedUser = userService.register(dto);
            
            try {
                // Gửi email xác nhận - nếu fail sẽ throw exception
                userService.sendVerificationEmail(savedUser.getId(), appBaseUrl);
            } catch (Exception emailException) {
                // Nếu gửi email fail → Delete user vừa tạo để rollback
                userService.deleteUser(savedUser.getId());
                model.addAttribute("errorMsg", 
                    "Không thể gửi email xác nhận. Vui lòng thử lại sau.");
                return "auth/register";
            }
            
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            return "auth/register";
        } catch (RuntimeException e) {
            model.addAttribute("errorMsg", e.getMessage());
            return "auth/register";
        }

        redirectAttributes.addFlashAttribute("successMsg",
                "Đăng ký thành công! Vui lòng kiểm tra email để xác nhận tài khoản.");
        redirectAttributes.addAttribute("email", dto.getEmail());
        return "redirect:/auth/check-email";
    }

    // ========================
    // GET /auth/check-email
    // ========================
    @GetMapping("/check-email")
    public String checkEmailPage(
            @RequestParam(required = false) String email,
            Model model) {
        model.addAttribute("email", email != null ? email : "");
        return "auth/check-email";
    }

    // ========================
    // GET /auth/verify-email
    // ========================
    @GetMapping("/verify-email")
    public String verifyEmail(
            @RequestParam String token,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            userService.verifyEmail(token);
            redirectAttributes.addFlashAttribute("successMsg",
                    "Email đã được xác nhận thành công! Bạn có thể đăng nhập ngay.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            return "auth/verify-error";
        }
    }

    // ========================
    // GET /auth/resend-verification-email
    // ========================
    @GetMapping("/resend-verification-email")
    public String resendVerificationEmail(
            @RequestParam(required = false) String email,
            Model model) {
        
        if (email == null || email.isEmpty()) {
            model.addAttribute("email", "");
            return "auth/resend-verification";
        }

        try {
            var user = userService.findByEmail(email.toLowerCase())
                    .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));
            
            if (user.getEmailVerified()) {
                model.addAttribute("errorMsg", "Email này đã được xác nhận rồi!");
                model.addAttribute("email", email);
                return "auth/resend-verification";
            }

            userService.sendVerificationEmail(user.getId(), appBaseUrl);
            model.addAttribute("successMsg", "Email xác nhận đã được gửi. Vui lòng kiểm tra hộp thư.");
            model.addAttribute("email", "");
            return "auth/resend-verification";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("email", email);
            return "auth/resend-verification";
        }
    }

    // ========================
    // GET /auth/forgot-password
    // ========================
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    // ========================
    // POST /auth/forgot-password
    // ========================
    @PostMapping("/forgot-password")
    public String forgotPassword(
            @RequestParam String email,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (email == null || email.trim().isEmpty()) {
            model.addAttribute("errorMsg", "Vui lòng nhập email!");
            return "auth/forgot-password";
        }

        try {
            userService.sendPasswordResetEmail(email.toLowerCase(), appBaseUrl);
            redirectAttributes.addFlashAttribute("successMsg",
                    "Email đặt lại mật khẩu đã được gửi. Vui lòng kiểm tra hộp thư của bạn.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("email", email);
            return "auth/forgot-password";
        } catch (Exception e) {
            model.addAttribute("errorMsg", "không thể gửi email. Vui lòng thử lại sau.");
            model.addAttribute("email", email);
            return "auth/forgot-password";
        }
    }

    // ========================
    // GET /auth/reset-password
    // ========================
    @GetMapping("/reset-password")
    public String resetPasswordPage(
            @RequestParam String token,
            Model model) {
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    // ========================
    // POST /auth/reset-password
    // ========================
    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Kiểm tra mật khẩu khớp nhau
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("errorMsg", "Mật khẩu xác nhận không khớp!");
            model.addAttribute("token", token);
            return "auth/reset-password";
        }

        // Kiểm tra độ dài mật khẩu
        if (newPassword.length() < 6 || newPassword.length() > 100) {
            model.addAttribute("errorMsg", "Mật khẩu phải từ 6-100 ký tự!");
            model.addAttribute("token", token);
            return "auth/reset-password";
        }

        try {
            userService.resetPassword(token, newPassword);
            redirectAttributes.addFlashAttribute("successMsg",
                    "Mật khẩu đã được đặt lại thành công! Bạn có thể đăng nhập với mật khẩu mới.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("token", token);
            return "auth/reset-password";
        } catch (Exception e) {
            model.addAttribute("errorMsg", "Có lỗi xảy ra. Vui lòng thử lại!");
            model.addAttribute("token", token);
            return "auth/reset-password";
        }
    }
}