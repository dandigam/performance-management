package com.rit.performance.controller;

import com.rit.performance.dto.request.TimesheetApprovalRequest;
import com.rit.performance.entity.TimesheetApprovalStatus;
import com.rit.performance.exception.GlobalExceptionHandler;
import com.rit.performance.service.impl.TimesheetApprovalService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TimesheetApprovalControllerTest {
    private final TimesheetApprovalService service = mock(TimesheetApprovalService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new TimesheetApprovalController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void routesPutAndUsesAuthenticatedIdentity() throws Exception {
        mvc.perform(put("/api/timesheets/1/approvals/10").principal(() -> "reviewer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APPROVED\",\"comments\":\"Checked\"}"))
                .andExpect(status().isOk());
        verify(service).update(1L, 10L, new TimesheetApprovalRequest(TimesheetApprovalStatus.APPROVED, "Checked"), "reviewer");
    }

    @Test
    void rejectsMalformedStatus() throws Exception {
        mvc.perform(put("/api/timesheets/1/approvals/10").principal(() -> "reviewer")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INVALID\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void returnsForbiddenForWrongApprover() throws Exception {
        when(service.update(anyLong(), anyLong(), any(), any())).thenThrow(new AccessDeniedException("Wrong approver"));
        mvc.perform(put("/api/timesheets/1/approvals/10").principal(() -> "reviewer")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());
    }
}
