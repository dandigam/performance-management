package com.rit.performance.controller;

import com.rit.performance.dto.AssignmentSowOptionResponse;
import com.rit.performance.service.AssignmentOptionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/assignments/options")
@RequiredArgsConstructor
public class AssignmentOptionsController {
    private final AssignmentOptionsService service;

    @GetMapping("/sows")
    public List<AssignmentSowOptionResponse> getSows() {
        return service.getSows();
    }
}
