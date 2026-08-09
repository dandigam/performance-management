package com.rit.performance.controller;

import com.rit.performance.dto.EmployeeBasicInfoResponse;
import com.rit.performance.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmployeeControllerTest {

    @Test
    void getsEmployeeById() {
        EmployeeService employeeService = mock(EmployeeService.class);
        EmployeeBasicInfoResponse employee = EmployeeBasicInfoResponse.builder().employeeId(1L).build();
        when(employeeService.getById(1L)).thenReturn(employee);

        ResponseEntity<EmployeeBasicInfoResponse> response =
                new EmployeeController(employeeService).getById(1L);

        assertSame(employee, response.getBody());
        verify(employeeService).getById(1L);
    }
}
