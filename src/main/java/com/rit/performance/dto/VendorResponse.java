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
    private String vendorCode;
    private String companyName;
    private String primaryContact;
    private String contactEmail;
    private String phoneNumber;
    private String paymentTerms;
    private String currency;
    private String status;
    private String address;
    private VendorPaymentDetailsResponse paymentDetails;
    private List<DocumentResponse> documentList;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
