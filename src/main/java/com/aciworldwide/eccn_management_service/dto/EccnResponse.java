package com.aciworldwide.eccn_management_service.dto;

import com.aciworldwide.eccn_management_service.model.Eccn;

import java.util.List;

public record EccnResponse(
    String id,
    String commodityCode,
    String category,
    String subCategory,
    List<String> controlReasons,
    String description,
    boolean financialSoftware,
    boolean deprecated
) {
    public static EccnResponse from(Eccn eccn) {
        return new EccnResponse(
            eccn.getId(),
            eccn.getCommodityCode(),
            eccn.getCategory(),
            eccn.getSubCategory(),
            eccn.getControlReasons(),
            eccn.getDescription(),
            eccn.isFinancialSoftware(),
            eccn.isDeprecated()
        );
    }
}
