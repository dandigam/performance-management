package com.rit.performance.mapper;

import com.rit.performance.dto.CsxEmployeeResponse;
import com.rit.performance.entity.CsxEmployee;

public final class CsxEmployeeMapper {
    private CsxEmployeeMapper() {
    }

    public static CsxEmployeeResponse toResponse(CsxEmployee employee) {
        String firstName = employee.getFirstName() == null ? "" : employee.getFirstName().trim();
        String lastName = employee.getLastName() == null ? "" : employee.getLastName().trim();
        return CsxEmployeeResponse.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .employeeName((firstName + " " + lastName).trim())
                .email(employee.getEmail())
                .phoneNumber(employee.getPhoneNumber())
                .designationId(employee.getDesignation() == null
                        ? null : employee.getDesignation().getId())
                .designationName(employee.getDesignation() == null
                        ? null : employee.getDesignation().getName())
                .businessUnitId(employee.getBusinessUnit() == null
                        ? null : employee.getBusinessUnit().getId())
                .businessUnitName(employee.getBusinessUnit() == null
                        ? null : employee.getBusinessUnit().getName())
                .status(employee.getStatus())
                .createdBy(employee.getCreatedBy())
                .createdDate(employee.getCreatedOn())
                .updatedBy(employee.getUpdatedBy())
                .updatedDate(employee.getUpdatedOn())
                .build();
    }
}
