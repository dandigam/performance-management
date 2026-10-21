package com.rit.performance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SowSignatureUpdateRequest {
    @NotBlank(message = "signedStatus is required")
    @Pattern(regexp = "(?i)(SIGNED|UNSIGNED)",
            message = "signedStatus must be SIGNED or UNSIGNED")
    private String signedStatus;

    private LocalDate signedDate;
}
