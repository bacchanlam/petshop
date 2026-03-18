package com.example.doan_petshop.service;

import com.example.doan_petshop.entity.Category;
import com.example.doan_petshop.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // Lấy tất cả danh mục (cho Admin)
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    // Lấy danh mục đang hiển thị (cho User)
    public List<Category> findVisible() {
        return categoryRepository.findByVisibleTrue();
    }

    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục id: " + id));
    }

    // ========================
    // THÊM danh mục
    // ========================
    @Transactional
    public Category save(Category category) {
        // Kiểm tra tên trùng (khi thêm mới)
        if (category.getId() == null &&
                categoryRepository.existsByNameIgnoreCase(category.getName())) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại");
        }
        // Kiểm tra tên trùng (khi sửa - loại trừ chính nó)
        if (category.getId() != null &&
                categoryRepository.existsByNameIgnoreCaseAndIdNot(category.getName(), category.getId())) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại");
        }
        return categoryRepository.save(category);
    }

    // ========================
    // XÓA danh mục
    // ========================
    @Transactional
    public void delete(Long id) {
        Category category = findById(id);
        // Nếu còn sản phẩm thì ẩn đi thay vì xóa
        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            throw new IllegalStateException(
                    "Danh mục còn " + category.getProducts().size() +
                            " sản phẩm. Hãy ẩn danh mục thay vì xóa."
            );
        }
        categoryRepository.deleteById(id);
    }

    // ========================
    // ẨN / HIỆN danh mục
    // ========================
    @Transactional
    public void toggleVisible(Long id) {
        Category category = findById(id);
        category.setVisible(!category.getVisible());
        categoryRepository.save(category);
    }
}