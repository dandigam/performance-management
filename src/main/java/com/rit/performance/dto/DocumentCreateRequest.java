package com.rit.performance.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DocumentCreateRequest {
    @Size(max = 255, message = "documentName must not exceed 255 characters")
    private String documentName;

    @Size(max = 255, message = "fileType must not exceed 255 characters")
    private String fileType;

    @Size(max = 255, message = "fileUrl must not exceed 255 characters")
    private String fileUrl;

    @Size(max = 255, message = "module must not exceed 255 characters")
    private String module;

    private LocalDateTime uploadedAt;
}
