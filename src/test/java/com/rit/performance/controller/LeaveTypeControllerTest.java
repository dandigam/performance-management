package com.rit.performance.controller;

import com.rit.performance.dto.response.LeaveTypeResponse;
import com.rit.performance.entity.*;
import com.rit.performance.exception.*;
import com.rit.performance.service.LeaveTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LeaveTypeControllerTest {
    private final LeaveTypeService service = mock(LeaveTypeService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new LeaveTypeController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    private final String body = "{\"code\":\"ANNUAL\",\"name\":\"Annual leave\",\"unit\":\"DAYS\",\"paid\":true}";

    @Test void createReturns201WithLocation() throws Exception {
        when(service.create(any())).thenReturn(new LeaveTypeResponse(1L,"ANNUAL","Annual leave",null,
                LeaveUnit.DAYS,true,LeaveTypeStatus.ACTIVE,null,null,null,null));
        mvc.perform(post("/api/v1/leave-types").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/v1/leave-types/1"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test void validatesRequiredFieldsAndEnums() throws Exception {
        for (String invalid : new String[]{"{}", body.replace("DAYS", "WEEKS"), body.replace("ANNUAL", " ")})
            mvc.perform(post("/api/v1/leave-types").contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/leave-types/1/status").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void routesReadsUpdateAndStatusChange() throws Exception {
        mvc.perform(get("/api/v1/leave-types")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/leave-types").param("status", "INACTIVE")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/leave-types/1")).andExpect(status().isOk());
        mvc.perform(put("/api/v1/leave-types/1").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/leave-types/1/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INACTIVE\"}")).andExpect(status().isOk());
        verify(service).getAll(null); verify(service).getAll(LeaveTypeStatus.INACTIVE);
        verify(service).getById(1L); verify(service).update(eq(1L), any());
        verify(service).changeStatus(1L, LeaveTypeStatus.INACTIVE);
    }

    @Test void missingAndDuplicateReturn404And409() throws Exception {
        when(service.getById(99L)).thenThrow(new ResourceNotFoundException("Leave type not found: 99"));
        mvc.perform(get("/api/v1/leave-types/99")).andExpect(status().isNotFound());
        when(service.create(any())).thenThrow(new DuplicateResourceException("Duplicate code"));
        mvc.perform(post("/api/v1/leave-types").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }
}
