package com.rit.performance.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowDocumentRequest {
    @NotNull(message = "document id is required")
    @Positive(message = "document id must be positive")
    private Long id;

    @Size(max = 255, message = "documentName must not exceed 255 characters")
    private String documentName;

    @Size(max = 255, message = "fileType must not exceed 255 characters")
    private String fileType;

    @Size(max = 255, message = "documentType must not exceed 255 characters")
    private String documentType;

    @Size(max = 255, message = "fileUrl must not exceed 255 characters")
    private String fileUrl;

    @Size(max = 255, message = "module must not exceed 255 characters")
    private String module;

    private LocalDateTime uploadedAt;
}
