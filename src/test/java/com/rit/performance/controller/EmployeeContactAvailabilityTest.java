package com.rit.performance.controller;

import com.rit.performance.exception.GlobalExceptionHandler;
import com.rit.performance.repository.EmployeeRepository;
import com.rit.performance.service.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EmployeeContactAvailabilityTest {
    @Mock EmployeeRepository employees;
    @InjectMocks EmployeeServiceImpl service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new EmployeeController(service, null, null))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void duplicateEmailReturnsRequestedWarningAfterNormalization() throws Exception {
        when(employees.existsByEmailIgnoreCase("venkatd099@gmail.com")).thenReturn(true);
        mvc.perform(get("/api/employees/check-contact").param("email", " VenkatD099@gmail.com "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("WARNING"))
                .andExpect(jsonPath("$.code").value("INVALID_OPERATION"))
                .andExpect(jsonPath("$.message").value("Employee email already exists: venkatd099@gmail.com"));
    }

    @Test
    void duplicatePhoneReturnsWarning() throws Exception {
        when(employees.existsByPhoneNumber("1234567890")).thenReturn(true);
        mvc.perform(get("/api/employees/check-contact").param("phoneNumber", " 1234567890 "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("WARNING"))
                .andExpect(jsonPath("$.code").value("INVALID_OPERATION"))
                .andExpect(jsonPath("$.message").value("Employee phone number already exists: 1234567890"));
    }

    @Test
    void updateExcludesCurrentEmployeeForBothFields() throws Exception {
        mvc.perform(get("/api/employees/check-contact").param("email", "person@example.com")
                        .param("phoneNumber", "1234567890").param("excludeEmployeeId", "42"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.type").value("SUCCESS"));
        verify(employees).existsByEmailIgnoreCaseAndIdNot("person@example.com", 42L);
        verify(employees).existsByPhoneNumberAndIdNot("1234567890", 42L);
        verifyNoMoreInteractions(employees);
    }

    @Test
    void updateStillRejectsAnotherEmployeesPhone() throws Exception {
        when(employees.existsByPhoneNumberAndIdNot("1234567890", 42L)).thenReturn(true);
        mvc.perform(get("/api/employees/check-contact").param("phoneNumber", "1234567890")
                        .param("excludeEmployeeId", "42"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_OPERATION"));
    }

    @Test
    void availableContactReturnsSuccess() throws Exception {
        mvc.perform(get("/api/employees/check-contact").param("email", "person@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Employee contact details are available"));
    }

    @Test
    void blankContactsAndInvalidExclusionAreRejectedWithoutQueries() throws Exception {
        mvc.perform(get("/api/employees/check-contact").param("email", " ").param("phoneNumber", " "))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.type").value("WARNING"));
        mvc.perform(get("/api/employees/check-contact").param("email", "person@example.com")
                        .param("excludeEmployeeId", "0"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(employees);
    }
}
