package com.example.doan_petshop.controller;

import com.example.doan_petshop.dto.ProductFilterDTO;
import com.example.doan_petshop.entity.Product;
import com.example.doan_petshop.enums.PetType;
import com.example.doan_petshop.service.CategoryService;
import com.example.doan_petshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService  productService;
    private final CategoryService categoryService;

    // ========================
    // GET /products - Danh sách sản phẩm + filter + phân trang
    // ========================
    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) PetType petType,
                       @RequestParam(required = false) String brand,
                       @RequestParam(required = false) BigDecimal minPrice,
                       @RequestParam(required = false) BigDecimal maxPrice,
                       @RequestParam(defaultValue = "newest") String sortBy,
                       @RequestParam(defaultValue = "0")  int page,
                       @RequestParam(defaultValue = "12") int size,
                       Model model) {

        Page<Product> products = productService.findByFilters(
                keyword, categoryId, petType, brand, minPrice, maxPrice, page, size, sortBy);

        // Tạo filter DTO để giữ giá trị trên form
        ProductFilterDTO filter = new ProductFilterDTO(
                keyword, categoryId, petType, brand, minPrice, maxPrice, sortBy, page, size);

        model.addAttribute("products",    products);
        model.addAttribute("filter",      filter);
        model.addAttribute("categories",  categoryService.findVisible());
        model.addAttribute("brands",      productService.findAllBrands());
        model.addAttribute("petTypes",    PetType.values());
        model.addAttribute("totalPages",  products.getTotalPages());
        model.addAttribute("currentPage", page);

        // Tiêu đề trang (hiển thị theo danh mục nếu có)
        if (categoryId != null) {
            try {
                String catName = categoryService.findById(categoryId).getName();
                model.addAttribute("pageTitle", catName);
            } catch (Exception ignored) {}
        } else if (keyword != null && !keyword.isBlank()) {
            model.addAttribute("pageTitle", "Kết quả: \"" + keyword + "\"");
        } else {
            model.addAttribute("pageTitle", "Tất cả sản phẩm");
        }

        return "user/product-list";
    }


    // ========================
    // GET /products/{id} - Chi tiết sản phẩm
    // ========================
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Product product = productService.findById(id);

        // Kiểm tra sản phẩm có active không
        if (!product.getActive()) {
            return "redirect:/products";
        }

        // Sản phẩm liên quan
        Long catId = product.getCategory() != null ? product.getCategory().getId() : null;
        if (catId != null) {
            model.addAttribute("relatedProducts",
                    productService.findRelated(catId, id, 6));
        }

        model.addAttribute("product",    product);
        model.addAttribute("categories", categoryService.findVisible());
        return "user/product-detail";
    }
}
