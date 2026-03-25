package com.example.doan_petshop.dto;

import com.example.doan_petshop.enums.PetType;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {

    private Long id;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 200, message = "Tên sản phẩm tối đa 200 ký tự")
    private String name;

    private String description;

    @NotNull(message = "Giá sản phẩm không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private BigDecimal price;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng không được âm")
    private Integer stock;

    @NotNull(message = "Vui lòng chọn loại thú cưng")
    private PetType petType;

    @Size(max = 100, message = "Thương hiệu tối đa 100 ký tự")
    private String brand;

    @NotNull(message = "Vui lòng chọn danh mục")
    private Long categoryId;

    // File ảnh đại diện upload mới
    private MultipartFile thumbnailFile;

    // Tên file thumbnail hiện tại (khi sửa)
    private String currentThumbnail;

    // Danh sách ảnh phụ upload thêm
    private List<MultipartFile> imageFiles;
}