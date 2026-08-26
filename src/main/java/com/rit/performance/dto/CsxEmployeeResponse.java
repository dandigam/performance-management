package com.rit.performance.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsxEmployeeResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String employeeName;
    private String email;
    private String phoneNumber;
    private Long designationId;
    private String designationName;
    private Long businessUnitId;
    private String businessUnitName;
    private String status;
    private Long createdBy;
    private LocalDateTime createdDate;
    private Long updatedBy;
    private LocalDateTime updatedDate;
}
