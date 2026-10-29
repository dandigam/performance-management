package com.rit.performance.dto;

import java.util.List;

public record EmployeeHierarchyImportResponse(
        String importId, String status, String message, int newEmployees,
        int insertedHierarchy, int updatedHierarchy, int unchanged, int errors,
        List<Row> rows) {
    public record Row(int rowNumber, String racfId, String firstName, String lastName,
                      String email, String managerRacfId, String employeeLocation, String gender,
                      String systemRole, String designation, String action, List<String> errors) {}
}
