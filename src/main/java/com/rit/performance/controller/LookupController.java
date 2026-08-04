package com.rit.performance.controller;

import com.rit.performance.dto.LookupTypeResponse;
import com.rit.performance.service.LookupService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/lookups")
public class LookupController {

    private final LookupService service;

    public LookupController(LookupService service) {
        this.service = service;
    }

    @GetMapping
    public List<LookupTypeResponse> getAll() {
        return service.getAllLookups();
    }
}
