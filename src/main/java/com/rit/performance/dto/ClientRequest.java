package com.rit.performance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ClientRequest {
    @NotBlank @Size(max = 200)
    private String clientName;
    @NotBlank @Size(max = 1000)
    private String clientAddress;
    @NotBlank @Size(max = 150)
    private String procurementPersonName;
    @NotBlank @Size(max = 30)
    private String procurementContactNumber;
    @NotBlank @Email @Size(max = 150)
    private String procurementEmail;
    @NotBlank @Size(max = 20)
    private String invoiceSubmissionType;
    @Email @Size(max = 150)
    private String invoiceSubmissionEmail;
    @NotBlank @Size(max = 150)
    private String vmoName;
    @NotBlank @Size(max = 30)
    private String vmoContactNumber;
    @NotBlank @Email @Size(max = 150)
    private String vmoEmail;
    @Size(max = 20)
    private String status;
    private List<@NotNull @Valid ClientDocumentRequest> documentList;
}
