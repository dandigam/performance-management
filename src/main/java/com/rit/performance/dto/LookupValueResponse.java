package com.rit.performance.dto;

public record LookupValueResponse(
        Long id,
        String code,
        String name,
        String description,
        int displayOrder,
        String status,
        boolean active
) {
}
