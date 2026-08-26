package com.rit.performance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientDocumentRequest {
    @NotNull(message = "document id is required")
    @Positive(message = "document id must be positive")
    private Long id;
}
