package com.rit.performance.controller;

import com.rit.performance.dto.EmailNotificationRequest;
import com.rit.performance.dto.EmailNotificationResponse;
import com.rit.performance.service.EmailDeliveryStatus;
import com.rit.performance.service.EmailEventType;
import com.rit.performance.service.EmailNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/email-notifications")
@RequiredArgsConstructor
public class EmailNotificationController {
    private final EmailNotificationService service;

    @PostMapping
    public ResponseEntity<EmailNotificationResponse> queue(@Valid @RequestBody EmailNotificationRequest request) {
        return ResponseEntity.ok(service.queueManual(request));
    }

    @GetMapping
    public ResponseEntity<Page<EmailNotificationResponse>> search(
            @RequestParam(required = false) EmailEventType eventType,
            @RequestParam(required = false) EmailDeliveryStatus status,
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) Long cycleId,
            @RequestParam(required = false) Long reviewId,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ResponseEntity.ok(service.search(eventType, status, recipient, cycleId, reviewId, query,
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdDate"))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailNotificationResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<EmailNotificationResponse> retry(@PathVariable Long id) {
        return ResponseEntity.ok(service.retry(id));
    }

    @GetMapping("/types")
    public ResponseEntity<List<EmailEventType>> types() {
        return ResponseEntity.ok(List.of(EmailEventType.values()));
    }

    @GetMapping("/statuses")
    public ResponseEntity<List<EmailDeliveryStatus>> statuses() {
        return ResponseEntity.ok(List.of(EmailDeliveryStatus.values()));
    }
}
