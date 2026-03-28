package com.example.doan_petshop.controller;

import com.example.doan_petshop.dto.ProductDTO;
import com.example.doan_petshop.dto.SiteNotification;
import com.example.doan_petshop.entity.Product;
import com.example.doan_petshop.enums.PetType;
import com.example.doan_petshop.service.CategoryService;
import com.example.doan_petshop .service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService         productService;
    private final CategoryService        categoryService;
    private final SimpMessagingTemplate  messagingTemplate;

    // ========================
    // GET - Danh sách sản phẩm (có tìm kiếm, phân trang)
    // ========================
    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(defaultValue = "0")  int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {

        Page<Product> products = productService.findByAdminFilters(keyword, categoryId, page, size);

        model.addAttribute("products",   products);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("keyword",    keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages",  products.getTotalPages());
        return "admin/products/list";
    }

    // ========================
    // GET - Form thêm mới
    // ========================
    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("productDTO", new ProductDTO());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("petTypes",   PetType.values());
        model.addAttribute("isEdit",     false);
        return "admin/products/form";
    }

    // ========================
    // POST - Lưu sản phẩm mới
    // ========================
    @PostMapping("/add")
    public String add(@Valid @ModelAttribute("productDTO") ProductDTO dto,
                      BindingResult bindingResult,
                      Model model,
                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("petTypes",   PetType.values());
            model.addAttribute("isEdit",     false);
            return "admin/products/form";
        }
        try {
            productService.create(dto);
            messagingTemplate.convertAndSend("/topic/site-updates",
                    new SiteNotification("PRODUCT_ADDED", null, "Sản phẩm mới vừa được thêm vào cửa hàng!"));
            redirectAttributes.addFlashAttribute("successMsg", "Thêm sản phẩm thành công!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi upload ảnh: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/products";
    }

    // ========================
    // GET - Form sửa sản phẩm
    // ========================
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Product product = productService.findById(id);

        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());
        dto.setPetType(product.getPetType());
        dto.setBrand(product.getBrand());
        dto.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
        dto.setCurrentThumbnail(product.getThumbnail());

        model.addAttribute("productDTO",  dto);
        model.addAttribute("product",     product);
        model.addAttribute("categories",  categoryService.findAll());
        model.addAttribute("petTypes",    PetType.values());
        model.addAttribute("isEdit",      true);
        return "admin/products/form";
    }

    // ========================
    // POST - Cập nhật sản phẩm
    // ========================
    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("productDTO") ProductDTO dto,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("petTypes",   PetType.values());
            model.addAttribute("isEdit",     true);
            return "admin/products/form";
        }
        try {
            productService.update(id, dto);
            messagingTemplate.convertAndSend("/topic/site-updates",
                    new SiteNotification("PRODUCT_UPDATED", id, "Thông tin sản phẩm vừa được cập nhật."));
            redirectAttributes.addFlashAttribute("successMsg", "Cập nhật sản phẩm thành công!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi upload ảnh: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    // ========================
    // POST - Ẩn / Hiện sản phẩm
    // ========================
    @PostMapping("/toggle/{id}")
    public String toggle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.toggleActive(id);
        Product p = productService.findById(id);
        String type = p.getActive() ? "PRODUCT_ACTIVATED" : "PRODUCT_DEACTIVATED";
        String msg  = p.getActive() ? "Sản phẩm \"" + p.getName() + "\" đã được kích hoạt."
                                    : "Sản phẩm \"" + p.getName() + "\" đã bị ẩn.";
        messagingTemplate.convertAndSend("/topic/site-updates", new SiteNotification(type, id, msg));
        redirectAttributes.addFlashAttribute("successMsg", "Đã cập nhật trạng thái sản phẩm.");
        return "redirect:/admin/products";
    }

    // ========================
    // POST - Xóa ảnh phụ
    // ========================
    @PostMapping("/images/delete/{imageId}")
    public String deleteImage(@PathVariable Long imageId,
                              @RequestParam Long productId,
                              RedirectAttributes redirectAttributes) {
        productService.deleteProductImage(imageId);
        redirectAttributes.addFlashAttribute("successMsg", "Đã xóa ảnh.");
        return "redirect:/admin/products/edit/" + productId;
    }

    // ========================
    // POST - Xóa sản phẩm
    // ========================
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.delete(id);
        messagingTemplate.convertAndSend("/topic/site-updates",
                new SiteNotification("PRODUCT_DELETED", id, "Một sản phẩm đã bị xóa."));
        redirectAttributes.addFlashAttribute("successMsg", "Đã xóa sản phẩm.");
        return "redirect:/admin/products";
    }
}
