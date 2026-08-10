package com.rit.performance.service;

import com.rit.performance.dto.DocumentCreateRequest;
import com.rit.performance.dto.DocumentResponse;
import com.rit.performance.entity.Document;
import com.rit.performance.exception.FileStorageException;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class DocumentService {
    private final DocumentRepository repository;
    private final Path storageDirectory;

    public DocumentService(
            DocumentRepository repository,
            @Value("${app.documents.storage-path}") String storagePath
    ) {
        this.repository = repository;
        this.storageDirectory = Path.of(storagePath).toAbsolutePath().normalize();
    }

    public DocumentResponse create(DocumentCreateRequest request) {
        Document document = Document.builder()
                .documentName(trimToNull(request.getDocumentName()))
                .fileType(trimToNull(request.getFileType()))
                .documentType(trimToNull(request.getDocumentType()))
                .fileUrl(trimToNull(request.getFileUrl()))
                .module(trimToNull(request.getModule()))
                .uploadedAt(request.getUploadedAt() == null
                        ? LocalDateTime.now() : request.getUploadedAt())
                .build();
        return toResponse(repository.save(document));
    }

    public DocumentResponse upload(MultipartFile file, String module, String documentType) {
        if (file == null || file.isEmpty()) {
            throw new InvalidOperationException("file is required and must not be empty");
        }

        String originalName = safeOriginalName(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + "_" + originalName;
        Path storedFile = storageDirectory.resolve(storedName).normalize();
        if (!storedFile.startsWith(storageDirectory)) {
            throw new InvalidOperationException("Invalid file name");
        }

        try {
            Files.createDirectories(storageDirectory);
            try (var input = file.getInputStream()) {
                Files.copy(input, storedFile);
            }

            Document document = Document.builder()
                    .documentName(originalName)
                    .fileType(trimToNull(file.getContentType()))
                    .documentType(trimToNull(documentType))
                    .fileUrl(storedFile.toString())
                    .module(trimToNull(module))
                    .uploadedAt(LocalDateTime.now())
                    .build();
            try {
                return toResponse(repository.saveAndFlush(document));
            } catch (RuntimeException ex) {
                Files.deleteIfExists(storedFile);
                throw ex;
            }
        } catch (IOException ex) {
            throw new FileStorageException("Unable to store uploaded file", ex);
        }
    }

    @Transactional(readOnly = true)
    public DocumentResponse get(Long id) {
        return toResponse(findDocument(id));
    }

    @Transactional(readOnly = true)
    public DocumentFile download(Long id) {
        Document document = findDocument(id);
        if (document.getFileUrl() == null || document.getFileUrl().isBlank()) {
            throw new ResourceNotFoundException("Document file is not available: " + id);
        }

        Path storedFile;
        try {
            storedFile = Path.of(document.getFileUrl()).toAbsolutePath().normalize();
        } catch (RuntimeException ex) {
            throw new ResourceNotFoundException("Document file is not available: " + id);
        }
        if (!storedFile.startsWith(storageDirectory) || !Files.isRegularFile(storedFile)) {
            throw new ResourceNotFoundException("Document file is not available: " + id);
        }

        Resource resource = new FileSystemResource(storedFile);
        String contentType = trimToNull(document.getFileType());
        if (contentType == null) {
            try {
                contentType = Files.probeContentType(storedFile);
            } catch (IOException ignored) {
                contentType = null;
            }
        }
        return new DocumentFile(
                resource,
                document.getDocumentName() == null
                        ? storedFile.getFileName().toString() : document.getDocumentName(),
                contentType == null ? "application/octet-stream" : contentType,
                storedFile.toFile().length());
    }

    private DocumentResponse toResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .documentName(document.getDocumentName())
                .fileType(document.getFileType())
                .documentType(document.getDocumentType())
                .fileUrl(document.getFileUrl())
                .module(document.getModule())
                .uploadedAt(document.getUploadedAt())
                .build();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String safeOriginalName(String originalName) {
        String name = originalName == null ? "file" : originalName.trim();
        name = name.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[^\\p{L}\\p{N}._-]", "_");
        if (name.isBlank() || ".".equals(name) || "..".equals(name)) name = "file";
        return name.length() <= 120 ? name : name.substring(name.length() - 120);
    }

    private Document findDocument(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
    }

    public record DocumentFile(
            Resource resource,
            String fileName,
            String contentType,
            long contentLength
    ) {
    }
}
