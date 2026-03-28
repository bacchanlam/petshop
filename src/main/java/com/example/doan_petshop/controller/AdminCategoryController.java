package com.example.doan_petshop.controller;

import com.example.doan_petshop.dto.SiteNotification;
import com.example.doan_petshop.entity.Category;
import com.example.doan_petshop.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService        categoryService;
    private final SimpMessagingTemplate  messagingTemplate;

    // ========================
    // GET - Danh sách danh mục
    // ========================
    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("newCategory", new Category());
        return "admin/categories/list";
    }

    // ========================
    // POST - Thêm danh mục mới
    // ========================
    @PostMapping("/add")
    public String add(@ModelAttribute("newCategory") Category category,
                      RedirectAttributes redirectAttributes) {
        try {
            categoryService.save(category);
            messagingTemplate.convertAndSend("/topic/site-updates",
                    new SiteNotification("CATEGORY_ADDED", null, "Danh mục mới vừa được thêm."));
            redirectAttributes.addFlashAttribute("successMsg", "Thêm danh mục thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    // ========================
    // GET - Form sửa danh mục
    // ========================
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("category", categoryService.findById(id));
        return "admin/categories/form";
    }

    // ========================
    // POST - Cập nhật danh mục
    // ========================
    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute Category category,
                         RedirectAttributes redirectAttributes) {
        try {
            category.setId(id);
            categoryService.save(category);
            messagingTemplate.convertAndSend("/topic/site-updates",
                    new SiteNotification("CATEGORY_UPDATED", id, "Danh mục vừa được cập nhật."));
            redirectAttributes.addFlashAttribute("successMsg", "Cập nhật danh mục thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    // ========================
    // POST - Ẩn / Hiện danh mục
    // ========================
    @PostMapping("/toggle/{id}")
    public String toggle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        categoryService.toggleVisible(id);
        messagingTemplate.convertAndSend("/topic/site-updates",
                new SiteNotification("CATEGORY_TOGGLED", id, "Danh mục vừa được cập nhật."));
        redirectAttributes.addFlashAttribute("successMsg", "Đã cập nhật trạng thái danh mục.");
        return "redirect:/admin/categories";
    }

    // ========================
    // POST - Xóa danh mục
    // ========================
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.delete(id);
            messagingTemplate.convertAndSend("/topic/site-updates",
                    new SiteNotification("CATEGORY_DELETED", id, "Một danh mục vừa bị xóa."));
            redirectAttributes.addFlashAttribute("successMsg", "Xóa danh mục thành công!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
