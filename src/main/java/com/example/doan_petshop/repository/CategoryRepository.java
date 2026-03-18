package com.example.doan_petshop.repository;

import com.example.doan_petshop.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Lấy danh mục đang hiện (visible = true) - dùng cho phía User
    List<Category> findByVisibleTrue();

    // Kiểm tra tên trùng
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}