package com.rit.performance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LookupValueUpsertRequest {
    private Long id;

    @NotBlank(message = "code is required")
    @Size(max = 50, message = "code must not exceed 50 characters")
    private String code;

    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must not exceed 100 characters")
    private String name;

    @Size(max = 20, message = "requirementType must not exceed 20 characters")
    private String requirementType;

    private String status;
    private Boolean active;
}
