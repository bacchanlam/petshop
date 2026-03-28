package com.example.doan_petshop.controller;

import com.example.doan_petshop.dto.ChangePasswordDTO;
import com.example.doan_petshop.security.CustomUserDetails;
import com.example.doan_petshop.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;

    // ========================
    // GET /account/profile
    // ========================
    @GetMapping("/profile")
    public String profilePage(@AuthenticationPrincipal CustomUserDetails userDetails,
                              Model model) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }
        // Load fresh từ DB để luôn hiện data mới nhất
        model.addAttribute("user", userService.findById(userDetails.getId()));
        return "user/account/profile";
    }

    // ========================
    // POST /account/profile
    // ========================
    @PostMapping("/profile")
    public String updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String fullName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        try {
            userService.updateProfile(userDetails.getId(), fullName, phone, address);

            // Sync lại object cached trong Security context để tránh hiện data cũ
            userDetails.getUser().setFullName(fullName);
            userDetails.getUser().setPhone(phone);
            userDetails.getUser().setAddress(address);

            redirectAttributes.addFlashAttribute("successMsg", "Cập nhật thông tin thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Có lỗi xảy ra, vui lòng thử lại.");
        }
        return "redirect:/account/profile";
    }

    // ========================
    // GET /account/change-password
    // ========================
    @GetMapping("/change-password")
    public String changePasswordPage(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     Model model) {
        // Nếu chưa đăng nhập, redirect về login
        if (userDetails == null) {
            return "redirect:/auth/login";
        }
        model.addAttribute("changePasswordDTO", new ChangePasswordDTO());
        model.addAttribute("user", userDetails.getUser());
        model.addAttribute("hasPassword", userDetails.getUser().getPassword() != null);
        return "user/account/change-password";
    }

    // ========================
    // POST /account/change-password
    // ========================
    @PostMapping("/change-password")
    public String changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute("changePasswordDTO") ChangePasswordDTO dto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        // Nếu chưa đăng nhập, redirect về login
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        if (bindingResult.hasErrors()) {
            return "user/account/change-password";
        }

        try {
            userService.changePassword(userDetails.getId(), dto);
            redirectAttributes.addFlashAttribute("successMsg", "Đổi mật khẩu thành công!");
            return "redirect:/account/profile";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/account/change-password";
        }
    }
}
