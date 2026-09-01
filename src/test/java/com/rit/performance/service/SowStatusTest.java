package com.rit.performance.service;

import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.service.impl.SowServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SowStatusTest {

    @Test
    void normalizesSupportedSowStatuses() {
        SowServiceImpl service = serviceWithoutDependencies();

        assertEquals("DRAFT", normalize(service, null));
        assertEquals("WAITING_FOR_APPROVAL", normalize(service, "Waiting for approval"));
        assertEquals("WAITING_FOR_APPROVAL", normalize(service, "WaitingForApproval"));
        assertEquals("ACTIVE", normalize(service, "Active"));
        assertEquals("ON_HOLD", normalize(service, "OnHold"));
        assertEquals("COMPLETED", normalize(service, "Completed"));
        assertEquals("CANCELLED", normalize(service, "Cancelled"));
    }

    @Test
    void rejectsUnsupportedSowStatus() {
        assertThrows(InvalidOperationException.class,
                () -> normalize(serviceWithoutDependencies(), "IN_PROGRESS"));
    }

    @Test
    void allowsPlanningAsMilestoneStatus() {
        SowServiceImpl service = serviceWithoutDependencies();

        assertEquals("PLANNING", ReflectionTestUtils.invokeMethod(
                service, "normalizeMilestoneStatus", "Planning"));
    }

    private String normalize(SowServiceImpl service, String value) {
        return ReflectionTestUtils.invokeMethod(service, "normalizeStatus", value, "DRAFT");
    }

    private SowServiceImpl serviceWithoutDependencies() {
        return new SowServiceImpl(null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }
}
