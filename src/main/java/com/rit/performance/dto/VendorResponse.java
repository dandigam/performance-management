package com.rit.performance.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorResponse {
    private Long id;
    private String companyName;
    private String vendorLocation;
    private String vendorType;
    private String taxIdentifier;
    private String taxIdentifierType;
    private String primaryContact;
    private String contactEmail;
    private String phoneNumber;
    private String paymentTerms;
    private String currency;
    private String status;
    private String address;
    private VendorBankDetailsResponse bankDetails;
    private List<DocumentResponse> documentList;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
