package com.rit.performance.dto;

import java.util.List;

public record LookupTypeResponse(
        Long id,
        String code,
        String name,
        String description,
        List<LookupValueResponse> values
) {
}
