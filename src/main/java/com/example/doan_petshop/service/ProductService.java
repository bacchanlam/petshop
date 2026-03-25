package com.example.doan_petshop.service;

import com.example.doan_petshop.dto.ProductDTO;
import com.example.doan_petshop.entity.*;
import com.example.doan_petshop.enums.PetType;
import com.example.doan_petshop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository     productRepository;
    private final CategoryRepository    categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final FileStorageService    fileStorageService;

    // ========================
    // ADMIN - Lấy danh sách có phân trang & lọc
    // ========================
    public Page<Product> findByAdminFilters(String keyword, Long categoryId,
                                            int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        Long  cId = (categoryId != null && categoryId != 0) ? categoryId : null;
        return productRepository.findByAdminFilters(kw, cId, pageable);
    }

    // ========================
    // USER - Lấy danh sách có filter, phân trang, sắp xếp
    // ========================
    public Page<Product> findByFilters(String keyword, Long categoryId, PetType petType,
                                       String brand, BigDecimal minPrice, BigDecimal maxPrice,
                                       int page, int size, String sortBy) {
        Sort sort = switch (sortBy == null ? "" : sortBy) {
            case "price_asc"  -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "newest"     -> Sort.by("createdAt").descending();
            default           -> Sort.by("createdAt").descending();
        };
        Pageable pageable = PageRequest.of(page, size, sort);

        String kw   = (keyword != null && !keyword.isBlank())   ? keyword.trim() : null;
        Long   cId  = (categoryId != null && categoryId != 0)   ? categoryId     : null;
        String br   = (brand != null && !brand.isBlank())       ? brand.trim()   : null;

        return productRepository.findByFilters(kw, cId, petType, br, minPrice, maxPrice, pageable);
    }

    // Sản phẩm liên quan
    public List<Product> findRelated(Long categoryId, Long excludeId, int limit) {
        return productRepository.findRelatedProducts(
                categoryId, excludeId, PageRequest.of(0, limit));
    }

    // Danh sách brand
    public List<String> findAllBrands() {
        return productRepository.findAllBrands();
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm id: " + id));
    }

    // ========================
    // THÊM sản phẩm mới
    // ========================
    @Transactional
    public Product create(ProductDTO dto) throws IOException {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        Product product = Product.builder()
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .stock(dto.getStock())
                .petType(dto.getPetType())
                .brand(dto.getBrand())
                .category(category)
                .active(true)
                .build();

        // Upload ảnh thumbnail
        if (dto.getThumbnailFile() != null && !dto.getThumbnailFile().isEmpty()) {
            String filename = fileStorageService.saveImage(dto.getThumbnailFile());
            product.setThumbnail(filename);
        }

        Product saved = productRepository.save(product);

        // Upload ảnh phụ
        saveExtraImages(saved, dto.getImageFiles());

        return saved;
    }

    // ========================
    // SỬA sản phẩm
    // ========================
    @Transactional
    public Product update(Long id, ProductDTO dto) throws IOException {
        Product product = findById(id);
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        product.setName(dto.getName().trim());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setPetType(dto.getPetType());
        product.setBrand(dto.getBrand());
        product.setCategory(category);

        // Nếu có upload ảnh thumbnail mới → xóa ảnh cũ, lưu ảnh mới
        if (dto.getThumbnailFile() != null && !dto.getThumbnailFile().isEmpty()) {
            fileStorageService.deleteImage(product.getThumbnail());
            String filename = fileStorageService.saveImage(dto.getThumbnailFile());
            product.setThumbnail(filename);
        }

        Product saved = productRepository.save(product);

        // Thêm ảnh phụ mới (không xóa ảnh cũ)
        saveExtraImages(saved, dto.getImageFiles());

        return saved;
    }

    // ========================
    // XÓA ảnh phụ
    // ========================
    @Transactional
    public void deleteProductImage(Long imageId) {
        ProductImage img = productImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ảnh"));
        fileStorageService.deleteImage(img.getImageUrl());
        productImageRepository.deleteById(imageId);
    }

    // ========================
    // ẨN / HIỆN sản phẩm
    // ========================
    @Transactional
    public void toggleActive(Long id) {
        Product product = findById(id);
        product.setActive(!product.getActive());
        productRepository.save(product);
    }

    // ========================
    // XÓA sản phẩm
    // ========================
    @Transactional
    public void delete(Long id) {
        Product product = findById(id);
        // Xóa ảnh
        fileStorageService.deleteImage(product.getThumbnail());
        product.getImages().forEach(img -> fileStorageService.deleteImage(img.getImageUrl()));
        productRepository.deleteById(id);
    }

    // ========================
    // CẬP NHẬT tồn kho
    // ========================
    @Transactional
    public void updateStock(Long id, int delta) {
        Product product = findById(id);
        int newStock = product.getStock() + delta;
        if (newStock < 0) throw new IllegalArgumentException("Tồn kho không đủ");
        product.setStock(newStock);
        productRepository.save(product);
    }

    // Dashboard
    public long countActive() { return productRepository.countByActiveTrue(); }

    public List<Product> findLowStock(int threshold) {
        return productRepository.findLowStockProducts(threshold);
    }

    // ========================
    // Helper - lưu ảnh phụ
    // ========================
    private void saveExtraImages(Product product, List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) return;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            String filename = fileStorageService.saveImage(file);
            ProductImage image = ProductImage.builder()
                    .product(product)
                    .imageUrl(filename)
                    .isMain(false)
                    .build();
            productImageRepository.save(image);
        }
    }
}