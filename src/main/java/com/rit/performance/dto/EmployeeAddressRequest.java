package com.rit.performance.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeAddressRequest {
    @Size(max = 200) private String addressLine1;
    @Size(max = 200) private String addressLine2;
    @Size(max = 100) private String city;
    @Size(max = 100) private String state;
    @Size(max = 20) private String postalCode;
    @Size(max = 100) private String country;
}
