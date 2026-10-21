package com.rit.performance.controller;

import com.rit.performance.service.TimesheetGenerationService;
import com.rit.performance.exception.GlobalExceptionHandler;
import com.rit.performance.exception.InvalidOperationException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TimesheetStatusControllerTest {
    private final TimesheetGenerationService service = mock(TimesheetGenerationService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new TimesheetController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void passesSelectedTabAndEmployeeToService() throws Exception {
        mvc.perform(get("/api/timesheets").param("employeeId", "3").param("status", "PENDING"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        verify(service).getAll(3L, "PENDING");
    }

    @Test
    void defaultsToAll() throws Exception {
        mvc.perform(get("/api/timesheets").param("employeeId", "3")).andExpect(status().isOk());
        verify(service).getAll(3L, "ALL");
    }

    @Test
    void invalidTabReturnsBadRequest() throws Exception {
        when(service.getAll(3L, "WRONG")).thenThrow(new InvalidOperationException("Invalid status"));
        mvc.perform(get("/api/timesheets").param("employeeId", "3").param("status", "WRONG"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emptyStatusDefaultsToAll() throws Exception {
        mvc.perform(get("/api/timesheets").param("employeeId", "2").param("status", ""))
                .andExpect(status().isOk());
        verify(service).getAll(2L, "ALL");
    }
}
