package com.rit.performance.service;

import com.rit.performance.dto.request.LeaveTypeRequest;
import com.rit.performance.entity.*;
import com.rit.performance.exception.*;
import com.rit.performance.repository.LeaveTypeRepository;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LeaveTypeServiceTest {
    private final LeaveTypeRepository repository = mock(LeaveTypeRepository.class);
    private final LeaveTypeService service = new LeaveTypeService(repository);
    private LeaveTypeRequest request() { return new LeaveTypeRequest(" annual ", " Annual leave ", " ", LeaveUnit.DAYS, true, null); }

    @Test void normalizesCodeAndPreservesAuditAndStatusOnUpdate() {
        when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        var result = service.create(request());
        assertEquals("ANNUAL", result.code()); assertEquals("Annual leave", result.name());
        assertEquals(LeaveTypeStatus.ACTIVE, result.status()); assertNull(result.description());
        var type = new LeaveType(); type.setId(1L); type.setCreatedBy(42L); type.setStatus(LeaveTypeStatus.INACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(type));
        result = service.update(1L, request());
        assertEquals(42L, result.createdBy()); assertEquals(LeaveTypeStatus.INACTIVE, result.status());
    }

    @Test void rejectsDuplicateCodesOnCreateAndUpdate() {
        when(repository.existsByCodeIgnoreCase("ANNUAL")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> service.create(request()));
        when(repository.findById(1L)).thenReturn(Optional.of(new LeaveType()));
        when(repository.existsByCodeIgnoreCaseAndIdNot("ANNUAL", 1L)).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> service.update(1L, request()));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test void translatesConcurrentMysqlDuplicate() {
        when(repository.saveAndFlush(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException(
                "duplicate", new java.sql.SQLException("duplicate", "23000", 1062)));
        assertThrows(DuplicateResourceException.class, () -> service.create(request()));
    }

    @Test void activatesAndDeactivatesWithoutDeleting() {
        var type = new LeaveType(); type.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(type));
        when(repository.saveAndFlush(type)).thenReturn(type);
        assertEquals(LeaveTypeStatus.INACTIVE, service.changeStatus(1L, LeaveTypeStatus.INACTIVE).status());
        assertEquals(LeaveTypeStatus.ACTIVE, service.changeStatus(1L, LeaveTypeStatus.ACTIVE).status());
        verify(repository, never()).delete(any()); verify(repository, never()).deleteById(any());
    }

    @Test void listsBothStatusesAndReportsMissingIds() {
        var inactive = new LeaveType(); inactive.setStatus(LeaveTypeStatus.INACTIVE);
        when(repository.findAllByOrderByNameAscIdAsc()).thenReturn(List.of(new LeaveType(), inactive));
        assertEquals(2, service.getAll(null).size());
        when(repository.findByStatusOrderByNameAscIdAsc(LeaveTypeStatus.INACTIVE)).thenReturn(List.of(inactive));
        assertEquals(1, service.getAll(LeaveTypeStatus.INACTIVE).size());
        assertThrows(ResourceNotFoundException.class, () -> service.getById(99L));
        assertThrows(ResourceNotFoundException.class, () -> service.changeStatus(99L, LeaveTypeStatus.ACTIVE));
    }
}
