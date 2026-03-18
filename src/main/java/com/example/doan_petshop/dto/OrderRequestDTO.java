package com.example.doan_petshop.dto;

import com.example.doan_petshop.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {

    @NotBlank(message = "Vui lòng nhập họ tên người nhận")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank(message = "Vui lòng nhập địa chỉ giao hàng")
    private String address;

    private String note;

    @NotNull(message = "Vui lòng chọn phương thức thanh toán")
    private PaymentMethod paymentMethod;
}