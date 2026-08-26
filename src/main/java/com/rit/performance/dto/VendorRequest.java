package com.rit.performance.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorRequest {

    @JsonAlias("legalCompanyName")
    @NotBlank(message = "companyName is required")
    @Size(max = 150, message = "companyName must not exceed 150 characters")
    private String companyName;

    @NotBlank(message = "vendorLocation is required")
    @Size(max = 20, message = "vendorLocation must not exceed 20 characters")
    private String vendorLocation;

    @NotBlank(message = "vendorType is required")
    @Size(max = 50, message = "vendorType must not exceed 50 characters")
    private String vendorType;

    @JsonAlias({"einNumber", "gstNumber", "gstEinNumber"})
    @NotBlank(message = "taxIdentifier is required")
    @Size(max = 50, message = "taxIdentifier must not exceed 50 characters")
    private String taxIdentifier;

    @Size(max = 100, message = "primaryContact must not exceed 100 characters")
    private String primaryContact;

    @Email(message = "contactEmail must be valid")
    @Size(max = 150, message = "contactEmail must not exceed 150 characters")
    private String contactEmail;

    @Size(max = 30, message = "phoneNumber must not exceed 30 characters")
    private String phoneNumber;

    @Size(max = 100, message = "paymentTerms must not exceed 100 characters")
    private String paymentTerms;

    private String currency;

    @Size(max = 20, message = "status must not exceed 20 characters")
    private String status;

    @Size(max = 500, message = "address must not exceed 500 characters")
    private String address;

    @JsonAlias("paymentDetails")
    @NotNull(message = "bankDetails is required")
    @Valid
    private VendorBankDetailsRequest bankDetails;

    private List<@NotNull(message = "documentList cannot contain null values")
            @Valid VendorDocumentRequest> documentList;
}
