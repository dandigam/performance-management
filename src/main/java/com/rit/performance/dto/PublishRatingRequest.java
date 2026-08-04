package com.rit.performance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublishRatingRequest {
    @NotNull(message = "publishedById is required")
    @Positive(message = "publishedById must be positive")
    private Long publishedById;
}
