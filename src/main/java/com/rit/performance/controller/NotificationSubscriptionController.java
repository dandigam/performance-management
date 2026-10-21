package com.rit.performance.controller;

import com.rit.performance.dto.NotificationSubscriptionRequest;
import com.rit.performance.dto.NotificationSubscriptionResponse;
import com.rit.performance.service.NotificationSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notification-subscriptions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class NotificationSubscriptionController {
    private final NotificationSubscriptionService service;

    @GetMapping
    public List<NotificationSubscriptionResponse> list() { return service.list(); }

    @GetMapping("/{id}")
    public NotificationSubscriptionResponse get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    public ResponseEntity<NotificationSubscriptionResponse> create(
            @Valid @RequestBody NotificationSubscriptionRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PutMapping("/{id}")
    public NotificationSubscriptionResponse update(@PathVariable Long id,
            @Valid @RequestBody NotificationSubscriptionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
