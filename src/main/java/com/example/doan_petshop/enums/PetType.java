package com.example.doan_petshop.enums;

public enum PetType {
    DOG("Chó"),
    CAT("Mèo"),
    DOG_AND_CAT("Chó và Mèo"),

    OTHER("Khác");

    private final String displayName;

    PetType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}