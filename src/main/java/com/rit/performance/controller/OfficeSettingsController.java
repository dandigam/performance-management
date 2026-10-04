package com.rit.performance.controller;
import com.rit.performance.dto.*;
import com.rit.performance.service.OfficeSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/settings")
public class OfficeSettingsController {
 private final OfficeSettingsService service;
 @GetMapping("/general") public GeneralSettingsResponse general() { return service.general(); }
 @PutMapping("/general") public GeneralSettingsResponse updateGeneral(@Valid @RequestBody GeneralSettingsRequest request) { return service.updateGeneral(request); }
 @GetMapping("/office-locations") public List<OfficeLocationResponse> list(@RequestParam(required=false) Boolean active,@RequestParam(required=false) String countryCode) { return service.list(active,countryCode); }
 @GetMapping("/office-locations/{id}") public OfficeLocationResponse get(@PathVariable Long id) { return service.get(id); }
 @PostMapping("/office-locations") public ResponseEntity<OfficeLocationResponse> create(@Valid @RequestBody OfficeLocationRequest request) {
  var response=service.create(request);
  return ResponseEntity.created(URI.create("/api/v1/settings/office-locations/"+response.id())).body(response);
 }
 @PutMapping("/office-locations/{id}") public OfficeLocationResponse update(@PathVariable Long id,@Valid @RequestBody OfficeLocationRequest request) { return service.update(id,request); }
}
