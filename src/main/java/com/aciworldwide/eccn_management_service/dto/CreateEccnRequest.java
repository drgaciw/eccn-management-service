package com.aciworldwide.eccn_management_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateEccnRequest(
    @NotBlank
    @Pattern(regexp = "^[0-9A-Z]{5}$", message = "ECCN code must be 5 characters of numbers and uppercase letters")
    String commodityCode,

    @NotBlank
    @Pattern(regexp = "^[0-9]$", message = "Category must be a single digit between 0 and 9")
    String category,

    @NotBlank
    @Pattern(regexp = "^[A-E]$", message = "Subcategory must be A, B, C, D, or E")
    String subCategory,

    @NotEmpty(message = "At least one control reason is required")
    List<String> controlReasons,

    @NotBlank
    @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
    String description,

    boolean financialSoftware
) {}
