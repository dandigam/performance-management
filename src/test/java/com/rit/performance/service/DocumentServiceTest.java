package com.rit.performance.service;

import com.rit.performance.dto.DocumentResponse;
import com.rit.performance.entity.Document;
import com.rit.performance.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentServiceTest {

    @TempDir
    Path storageDirectory;

    @Test
    void storesMultipartFileAndPersistsMetadata() {
        DocumentRepository repository = mock(DocumentRepository.class);
        when(repository.saveAndFlush(any(Document.class))).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            document.setId(1L);
            return document;
        });
        DocumentService service = new DocumentService(
                repository, storageDirectory.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../SOW Agreement.pdf",
                "application/pdf",
                "test document".getBytes());

        DocumentResponse response = service.upload(file, "SOW", "Signed NDA / MSA");

        assertEquals(1L, response.getId());
        assertEquals("SOW_Agreement.pdf", response.getDocumentName());
        assertEquals("application/pdf", response.getFileType());
        assertEquals("Signed NDA / MSA", response.getDocumentType());
        assertEquals("SOW", response.getModule());
        assertTrue(Files.exists(Path.of(response.getFileUrl())));
        assertTrue(Path.of(response.getFileUrl()).startsWith(storageDirectory));

        Document stored = Document.builder()
                .id(response.getId())
                .documentName(response.getDocumentName())
                .fileType(response.getFileType())
                .documentType(response.getDocumentType())
                .fileUrl(response.getFileUrl())
                .module(response.getModule())
                .uploadedAt(response.getUploadedAt())
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(stored));

        DocumentResponse metadata = service.get(1L);
        DocumentService.DocumentFile download = service.download(1L);

        assertEquals("SOW_Agreement.pdf", metadata.getDocumentName());
        assertEquals("Signed NDA / MSA", metadata.getDocumentType());
        assertEquals("SOW_Agreement.pdf", download.fileName());
        assertEquals("application/pdf", download.contentType());
        assertEquals("test document".getBytes().length, download.contentLength());
    }
}
