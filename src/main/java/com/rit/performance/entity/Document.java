package com.rit.performance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_name", length = 255)
    private String documentName;

    @Column(name = "file_type", length = 255)
    private String fileType;

    @Column(name = "document_type", length = 255)
    private String documentType;

    @Column(name = "file_url", length = 255)
    private String fileUrl;

    @Column(length = 255)
    private String module;

    @Column(name = "uploaded_at", columnDefinition = "DATETIME(6)")
    private LocalDateTime uploadedAt;
}
