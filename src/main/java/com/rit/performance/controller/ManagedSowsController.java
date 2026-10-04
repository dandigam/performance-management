package com.rit.performance.controller;

import com.rit.performance.dto.response.ManagedSowsResponse;
import com.rit.performance.service.ManagedSowsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ManagedSowsController {
    private final ManagedSowsService service;

    @GetMapping("/api/v1/sows/my-managed")
    public ManagedSowsResponse getMyManagedSows() {
        return service.getMyManagedSows();
    }
}
