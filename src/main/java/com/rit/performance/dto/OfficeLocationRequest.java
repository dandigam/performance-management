package com.rit.performance.dto;
import jakarta.validation.constraints.*;
public record OfficeLocationRequest(
 @NotBlank @Size(max=150) String officeName,
 @NotBlank @Size(max=255) String addressLine1,
 @Size(max=255) String addressLine2,
 @NotBlank @Size(max=100) String city,
 @Size(max=100) String stateRegion,
 @Size(max=20) String postalCode,
 @NotBlank @Pattern(regexp="[a-zA-Z]{2}",message="countryCode must be a two-letter country code") String countryCode,
 @NotBlank @Size(max=100) String timeZone,
 @Size(max=30) String phone,
 @Email @Size(max=254) String email,
 @NotNull Boolean active) {}
