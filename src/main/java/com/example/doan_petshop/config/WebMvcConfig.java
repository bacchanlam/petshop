package com.example.doan_petshop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/products}")
    private String uploadDir;

    // Ánh xạ URL /uploads/products/** → thư mục vật lý uploads/products/
    // Nhờ đó Thymeleaf dùng <img th:src="@{/uploads/products/abc.jpg}"> là hiển thị được
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get(uploadDir).toAbsolutePath().toUri().toString();

        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations(absolutePath);

        // Ảnh tĩnh trong resources/static
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
    }
}