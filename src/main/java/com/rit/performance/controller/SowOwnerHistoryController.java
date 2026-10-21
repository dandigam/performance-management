package com.rit.performance.controller;

import com.rit.performance.dto.request.SowOwnersUpdateRequest;
import com.rit.performance.dto.response.*;
import com.rit.performance.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/sows")
public class SowOwnerHistoryController {
    private final SowOwnerHistoryService history;
    private final SowService sows;
    @GetMapping("/{sowId}/owner-history")
    public List<SowOwnerHistoryResponse> list(@PathVariable Long sowId) { return history.list(sowId); }
    @PutMapping("/{sowId}/owners")
    public SowResponse update(@PathVariable Long sowId, @Valid @RequestBody SowOwnersUpdateRequest request) {
        return sows.updateOwners(sowId, request);
    }
}
