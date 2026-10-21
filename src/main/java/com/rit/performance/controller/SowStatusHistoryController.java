package com.rit.performance.controller;

import com.rit.performance.dto.response.SowStatusHistoryResponse;
import com.rit.performance.service.SowStatusHistoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sows")
public class SowStatusHistoryController {
    private final SowStatusHistoryQueryService service;

    @GetMapping("/{sowId}/status-history")
    public List<SowStatusHistoryResponse> list(@PathVariable Long sowId) {
        return service.list(sowId);
    }
}
