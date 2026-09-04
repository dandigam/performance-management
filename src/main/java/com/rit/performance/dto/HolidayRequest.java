package com.rit.performance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class HolidayRequest {
    @NotBlank(message = "holidayName is required")
    @Size(max = 150)
    private String holidayName;

    @NotNull(message = "holidayDate is required")
    private LocalDate holidayDate;

    @NotBlank(message = "locationType is required")
    private String locationType;

    @Size(max = 500)
    private String description;

    private Boolean active;
}
