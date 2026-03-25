package com.example.doan_petshop.dto;

import com.example.doan_petshop.enums.PetType;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterDTO {

    private String     keyword;
    private Long       categoryId;
    private PetType    petType;
    private String     brand;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String     sortBy;
    private int        page = 0;
    private int        size = 12;
}
