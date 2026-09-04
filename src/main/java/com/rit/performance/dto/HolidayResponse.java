package com.rit.performance.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class HolidayResponse {
    private Long id;
    private String holidayName;
    private LocalDate holidayDate;
    private String locationType;
    private String description;
    private boolean active;
    private Long createdBy;
    private LocalDateTime createdOn;
    private Long updatedBy;
    private LocalDateTime updatedOn;
}
