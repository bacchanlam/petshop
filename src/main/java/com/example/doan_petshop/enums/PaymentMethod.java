package com.example.doan_petshop.enums;

public enum PaymentMethod {
    COD("Thanh toán khi nhận hàng"),
    TRANSFER("Chuyển khoản ngân hàng"),
    MOMO("Thanh toán qua MoMo");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
