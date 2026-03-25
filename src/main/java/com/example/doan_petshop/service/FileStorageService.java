package com.example.doan_petshop.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads/products}")
    private String uploadDir;

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB

    // ========================
    // Lưu file ảnh, trả về tên file đã lưu
    // ========================
    public String saveImage(MultipartFile file) throws IOException {
        // Kiểm tra file rỗng
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }
        // Kiểm tra định dạng
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh (jpg, png, webp, gif)");
        }
        // Kiểm tra kích thước
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("File không được vượt quá 5MB");
        }

        // Tạo thư mục nếu chưa có
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Tạo tên file unique để tránh trùng
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFilename = UUID.randomUUID().toString() + extension;

        // Lưu file
        Path filePath = uploadPath.resolve(newFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return newFilename;
    }

    // ========================
    // Xóa file ảnh
    // ========================
    public void deleteImage(String filename) {
        if (filename == null || filename.isBlank()) return;
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log lỗi nhưng không throw - không để lỗi xóa ảnh block flow chính
            System.err.println("Không thể xóa file: " + filename + " - " + e.getMessage());
        }
    }

    // ========================
    // Lấy URL để hiển thị trong Thymeleaf
    // ========================
    public String getImageUrl(String filename) {
        if (filename == null || filename.isBlank()) return "/images/no-image.png";
        return "/" + uploadDir + "/" + filename;
    }
}