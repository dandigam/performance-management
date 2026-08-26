package com.rit.performance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentSetupRequest {

    private Long cycleId;

    @NotEmpty(message = "sections must contain at least one section")
    private List<@Valid AssessmentSectionRequest> sections;
}
