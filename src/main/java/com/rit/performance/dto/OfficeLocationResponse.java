package com.rit.performance.dto;
import java.time.LocalDateTime;
public record OfficeLocationResponse(Long id,String officeName,String addressLine1,String addressLine2,
 String city,String stateRegion,String postalCode,String countryCode,String timeZone,
 String phone,String email,boolean active,boolean mainOffice,LocalDateTime createdOn,LocalDateTime updatedOn) {}
