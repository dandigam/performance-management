package com.rit.performance.dto;
import jakarta.validation.constraints.*;
public record GeneralSettingsRequest(@NotBlank @Size(max=150) String portalName,@Positive Long mainOfficeId) {}
