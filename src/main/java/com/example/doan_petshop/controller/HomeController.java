package com.example.doan_petshop.controller;

import com.example.doan_petshop.entity.Product;
import com.example.doan_petshop.repository.ProductRepository;
import com.example.doan_petshop.service.CategoryService;
import com.example.doan_petshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService    productService;
    private final CategoryService   categoryService;
    private final ProductRepository productRepository;

    @GetMapping("/")
    public String home(Model model) {
        
        // Sản phẩm mới nhất
        model.addAttribute("newProducts",
                productService.findByFilters(null, null, null, null, null, null,
                        0, 8, "newest").getContent());

        // Danh mục
        model.addAttribute("categories", categoryService.findVisible());

        // Sản phẩm sắp hết hàng
        model.addAttribute("lowStockProducts", productService.findLowStock(5));

        // ── BESTSELLER ──────────────────────────────────────────
        // JPQL trả về Object[]{Product, Long} — row[0] là Product, row[1] là soldCount
        List<Object[]> rawRows = productRepository.findBestSellersRaw(PageRequest.of(0, 8));

        List<Product> bestSellers;

        if (rawRows != null && !rawRows.isEmpty()) {
            bestSellers = rawRows.stream().map(row -> {
                Product p = (Product) row[0];              // row[0] = Product entity
                Long sold = ((Number) row[1]).longValue(); // row[1] = SUM(quantity)
                p.setSoldCount(sold.intValue());
                return p;
            }).collect(Collectors.toList());

            // Fallback random nếu chưa có đơn nào
            boolean noSalesYet = bestSellers.stream()
                    .allMatch(p -> p.getSoldCount() == null || p.getSoldCount() == 0);
            if (noSalesYet) {
                bestSellers = productRepository.findRandom8(PageRequest.of(0, 8));
            }
        } else {
            bestSellers = productRepository.findRandom8(PageRequest.of(0, 8));
        }

        model.addAttribute("bestSellerProducts", bestSellers);
        // ────────────────────────────────────────────────────────

        // Chỉ hiển thị contact-float và chat-widget trên trang chủ
        model.addAttribute("showHomeWidgets", true);

        return "user/home";
    }

    @GetMapping("/about")
    public String about() { return "pages/about"; }

    @GetMapping("/membership")
    public String membership() { return "pages/membership"; }

    @GetMapping("/terms")
    public String terms() { return "pages/terms"; }

    @GetMapping("/privacy-policy")
    public String privacypolicy() { return "pages/privacy-policy"; }

    @GetMapping("/return-policy")
    public String returnpoolicy() { return "pages/return-policy"; }

}