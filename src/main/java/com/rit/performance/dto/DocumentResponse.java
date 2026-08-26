package com.rit.performance.dto;

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
public class DocumentResponse {
    private Long id;
    private String documentName;
    private String fileType;
    private String documentType;
    private String fileUrl;
    private String module;
    private LocalDateTime uploadedAt;
}
