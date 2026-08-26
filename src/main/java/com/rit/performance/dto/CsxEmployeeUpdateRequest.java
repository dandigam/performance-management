package com.rit.performance.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsxEmployeeUpdateRequest {
    @Positive(message = "id must be positive")
    private Long id;

    @NotBlank(message = "firstName is required")
    @Size(max = 50, message = "firstName must not exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "lastName must not exceed 50 characters")
    private String lastName;

    @Email(message = "email must be valid")
    @Size(max = 100, message = "email must not exceed 100 characters")
    private String email;

    @Size(max = 20, message = "phoneNumber must not exceed 20 characters")
    private String phoneNumber;

    @Positive(message = "designationId must be positive")
    private Long designationId;

    @Positive(message = "businessUnitId must be positive")
    private Long businessUnitId;

    @Size(max = 20, message = "status must not exceed 20 characters")
    private String status;
}
