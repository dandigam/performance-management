package com.rit.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientResponse {
    private Long id;
    private String clientName;
    private String clientAddress;
    private String procurementPersonName;
    private String procurementContactNumber;
    private String procurementEmail;
    private String invoiceSubmissionType;
    private String invoiceSubmissionEmail;
    private String vmoName;
    private String vmoContactNumber;
    private String vmoEmail;
    private String status;
    private List<DocumentResponse> documentList;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
