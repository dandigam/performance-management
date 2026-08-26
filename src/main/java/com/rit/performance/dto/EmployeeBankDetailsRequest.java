package com.rit.performance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeBankDetailsRequest {
    @NotBlank
    @Size(max = 100)
    private String bankCountry;

    @NotBlank
    @Pattern(regexp = "[A-Za-z]{3}", message = "currency must be a 3-letter code")
    private String currency;

    @NotBlank
    @Size(max = 200)
    private String accountHolderName;

    @NotBlank
    @Size(max = 200)
    private String bankName;

    @Size(max = 100)
    private String accountNumber;

    @NotBlank
    @Size(max = 20)
    private String ifscCode;
}
