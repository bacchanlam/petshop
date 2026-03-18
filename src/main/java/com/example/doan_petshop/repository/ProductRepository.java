package com.example.doan_petshop.repository;

import com.example.doan_petshop.entity.Product;
import com.example.doan_petshop.enums.PetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
        SELECT p FROM Product p
        WHERE p.active = true
          AND (:keyword  IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:petType  IS NULL OR p.petType = :petType)
          AND (:brand    IS NULL OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :brand, '%')))
          AND (:minPrice IS NULL OR p.price >= :minPrice)
          AND (:maxPrice IS NULL OR p.price <= :maxPrice)
    """)
    Page<Product> findByFilters(
            @Param("keyword")    String keyword,
            @Param("categoryId") Long categoryId,
            @Param("petType")    PetType petType,
            @Param("brand")      String brand,
            @Param("minPrice")   BigDecimal minPrice,
            @Param("maxPrice")   BigDecimal maxPrice,
            Pageable pageable
    );

    @Query("""
        SELECT p FROM Product p
        WHERE p.active = true
          AND p.category.id = :categoryId
          AND p.id <> :excludeId
        ORDER BY RAND()
    """)
    List<Product> findRelatedProducts(
            @Param("categoryId") Long categoryId,
            @Param("excludeId")  Long excludeId,
            Pageable pageable
    );

    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.active = true AND p.brand IS NOT NULL ORDER BY p.brand")
    List<String> findAllBrands();

    @Query("""
        SELECT p FROM Product p
        WHERE (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
    """)
    Page<Product> findByAdminFilters(
            @Param("keyword")    String keyword,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    long countByActiveTrue();

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stock <= :threshold ORDER BY p.stock ASC")
    List<Product> findLowStockProducts(@Param("threshold") int threshold);

    // ========================
    // Bestseller: trả về [Product, soldCount] dùng JPQL (không phải native)
    // ========================
    @Query("""
        SELECT p, COALESCE(SUM(oi.quantity), 0)
        FROM Product p
        LEFT JOIN OrderItem oi ON oi.product = p
        WHERE p.active = true
        GROUP BY p
        ORDER BY COALESCE(SUM(oi.quantity), 0) DESC
    """)
    List<Object[]> findBestSellersRaw(Pageable pageable);

    // Fallback random
    @Query("SELECT p FROM Product p WHERE p.active = true ORDER BY RAND()")
    List<Product> findRandom8(Pageable pageable);
}