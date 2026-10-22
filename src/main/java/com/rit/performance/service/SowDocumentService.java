package com.rit.performance.service;

import com.rit.performance.dto.request.SowDocumentRequest;
import com.rit.performance.entity.Document;
import com.rit.performance.entity.Sow;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.DocumentRepository;
import com.rit.performance.repository.SowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SowDocumentService {
    private final SowRepository sows;
    private final DocumentRepository documents;

    @Transactional
    public void attach(Long sowId, List<SowDocumentRequest> requested) {
        // Serialize attachment changes so concurrent requests cannot overwrite links.
        Sow sow = sows.findForStatusUpdate(sowId)
                .orElseThrow(() -> new ResourceNotFoundException("SOW not found: " + sowId));
        Set<Long> ids = requested.stream().map(SowDocumentRequest::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Document> stored = documents.findAllById(ids);
        Set<Long> found = stored.stream().map(Document::getId).collect(Collectors.toSet());
        for (Long id : ids) {
            if (!found.contains(id)) {
                throw new ResourceNotFoundException("Document not found: " + id);
            }
        }
        Set<Long> attached = sow.getDocuments().stream().map(Document::getId)
                .collect(Collectors.toCollection(HashSet::new));
        // Trust stored metadata, never the client-supplied name, path, or timestamps.
        stored.stream().filter(document -> attached.add(document.getId()))
                .forEach(sow.getDocuments()::add);
    }
}
