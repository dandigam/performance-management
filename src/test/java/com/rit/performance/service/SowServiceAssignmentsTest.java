package com.rit.performance.service;

import com.rit.performance.dto.request.SowAssignmentUpdateRequest;
import com.rit.performance.dto.SowRequirementMilestonesResponse;
import com.rit.performance.entity.*;
import com.rit.performance.repository.*;
import com.rit.performance.service.impl.SowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SowServiceAssignmentsTest {
    @Mock private SowRepository sowRepository;
    @Mock private EmployeeAssignmentRepository assignmentRepository;
    @Mock private SowMilestoneRepository milestoneRepository;
    @Mock private SowInvoiceService sowInvoiceService;
    @Mock private SowFeatureRepository featureRepository;
    @Mock private LookupValueRepository lookupValueRepository;
    @Mock private RateCardRepository rateCardRepository;
    @Mock private SowMilestonePositionAssignmentRepository positionAssignmentRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private CsxEmployeeRepository csxEmployeeRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private SowResourceRequirementService resourceRequirementService;

    private SowServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SowServiceImpl(sowRepository, assignmentRepository, milestoneRepository,
                sowInvoiceService, featureRepository, lookupValueRepository, rateCardRepository,
                positionAssignmentRepository, employeeRepository, csxEmployeeRepository,
                documentRepository, userRepository, clientRepository, resourceRequirementService);
    }

    @Test
    void returnsActiveAssignmentsAcrossAllSows() {
        Sow sow = Sow.builder().id(7L).sowCode("SW001").sowName("UMTS").build();
        SowMilestone milestone = SowMilestone.builder().id(4L).milestoneName("UI Changes")
                .startDate(LocalDate.of(2026, 8, 1)).endDate(LocalDate.of(2026, 8, 31)).build();
        sow.addMilestone(milestone);
        Employee employee = employee(3L, "Putsala", "Swamy");
        Employee lead = employee(1L, "Venkatesh", "Dandigam");
        Employee manager = employee(2L, "Charan", "Kovvuru");
        EmployeeAssignment assignment = new EmployeeAssignment();
        assignment.setId(15L);
        assignment.setEmployeeId(3L);
        assignment.setSowId(7L);
        assignment.setMilestoneId(4L);
        assignment.setDesignationId(20L);
        assignment.setPositionType("BILLABLE");
        assignment.setLeadId(1L);
        assignment.setManagerId(2L);
        assignment.setStatus("ACTIVE");

        when(sowRepository.findAllWithDetails()).thenReturn(List.of(sow));
        when(assignmentRepository.findByStatusIgnoreCaseOrderByIsPrimaryAssignmentDescEffectiveFromDesc("ACTIVE"))
                .thenReturn(List.of(assignment));
        when(employeeRepository.findByIdIn(any())).thenReturn(List.of(employee, lead, manager));
        when(lookupValueRepository.findAllById(any()))
                .thenReturn(List.of(LookupValue.builder().id(20L)
                        .name("Senior Software Engineer").build()));

        var response = service.getAllAssignments();

        assertEquals(1, response.size());
        assertEquals("Putsala Swamy", response.get(0).getEmployeeName());
        assertEquals("UMTS", response.get(0).getSowName());
        assertEquals("UI Changes", response.get(0).getMilestoneName());
        assertEquals("Senior Software Engineer", response.get(0).getDesignationName());
        assertEquals("BILLABLE", response.get(0).getPositionType());
        assertEquals("Venkatesh Dandigam", response.get(0).getLeadName());
        assertEquals("Charan Kovvuru", response.get(0).getManagerName());
    }

    @Test
    void returnsMilestonesContainingRequestedPositionTitle() {
        Long sowId = 7L;
        Long positionId = 21L;
        LookupType designationType = LookupType.builder().code("DESIGNATION").build();
        LookupValue position = LookupValue.builder()
                .id(positionId).lookupType(designationType).name("Technical Lead").build();
        SowRequirementMilestonesResponse details = SowRequirementMilestonesResponse.builder()
                .sowId(sowId).requirementId(1L).positionId(positionId).build();
        when(sowRepository.existsById(sowId)).thenReturn(true);
        when(lookupValueRepository.findById(positionId)).thenReturn(Optional.of(position));
        when(resourceRequirementService.getMilestonesByPosition(sowId, positionId))
                .thenReturn(details);

        var result = service.getMilestonesByPosition(sowId, positionId);

        assertEquals(sowId, result.getSowId());
        assertEquals(1L, result.getRequirementId());
        assertEquals(positionId, result.getPositionId());
    }

    @Test
    void editsAnActiveAssignment() {
        Sow sow = Sow.builder().id(7L).sowCode("SW001").sowName("UMTS").build();
        Employee employee = activeEmployee(3L, "Putsala", "Swamy");
        Employee lead = activeEmployee(1L, "Venkatesh", "Dandigam");
        Employee manager = activeEmployee(2L, "Charan", "Kovvuru");
        EmployeeAssignment assignment = assignment();
        EmployeeAssignment previousPrimary = assignment();
        previousPrimary.setId(14L);
        previousPrimary.setIsPrimaryAssignment(true);
        SowAssignmentUpdateRequest request = updateRequest("ACTIVE", null, true);

        stubUpdateLookups(sow, employee, lead, manager, assignment);
        when(assignmentRepository.existsBySowIdAndEmployeeIdAndStatusIgnoreCaseAndIdNot(
                7L, 3L, "ACTIVE", 15L)).thenReturn(false);
        when(assignmentRepository.findAllByEmployeeIdAndStatusIgnoreCase(3L, "ACTIVE"))
                .thenReturn(List.of(assignment, previousPrimary));

        var response = service.updateAssignment(15L, request);

        assertEquals("ACTIVE", response.getAssignmentStatus());
        assertTrue(response.getIsPrimaryAssignment());
        assertNull(response.getAssignmentEndDate());
        assertFalse(previousPrimary.getIsPrimaryAssignment());
        verify(assignmentRepository).saveAll(List.of(previousPrimary));
    }

    @Test
    void completesAnAssignmentAndClearsItsPrimaryFlag() {
        Sow sow = Sow.builder().id(7L).sowCode("SW001").sowName("UMTS").build();
        Employee employee = activeEmployee(3L, "Putsala", "Swamy");
        Employee lead = activeEmployee(1L, "Venkatesh", "Dandigam");
        Employee manager = activeEmployee(2L, "Charan", "Kovvuru");
        EmployeeAssignment assignment = assignment();
        SowAssignmentUpdateRequest request = updateRequest(
                "COMPLETED", LocalDate.of(2026, 8, 15), true);

        stubUpdateLookups(sow, employee, lead, manager, assignment);

        var response = service.updateAssignment(15L, request);

        assertEquals("COMPLETED", response.getAssignmentStatus());
        assertEquals(LocalDate.of(2026, 8, 15), response.getAssignmentEndDate());
        assertFalse(response.getIsPrimaryAssignment());
    }

    private void stubUpdateLookups(Sow sow, Employee employee, Employee lead,
                                   Employee manager, EmployeeAssignment assignment) {
        when(assignmentRepository.findById(15L)).thenReturn(Optional.of(assignment));
        when(sowRepository.findByIdWithDetails(7L)).thenReturn(Optional.of(sow));
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(employee));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(lead));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(lookupValueRepository.findById(20L)).thenReturn(Optional.of(
                LookupValue.builder().id(20L).name("Senior Software Engineer").build()));
        when(assignmentRepository.save(any(EmployeeAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(employeeRepository.findByIdIn(any())).thenReturn(List.of(employee, lead, manager));
        when(lookupValueRepository.findAllById(any())).thenReturn(List.of(
                LookupValue.builder().id(20L).name("Senior Software Engineer").build()));
    }

    private EmployeeAssignment assignment() {
        EmployeeAssignment assignment = new EmployeeAssignment();
        assignment.setId(15L);
        assignment.setEmployeeId(3L);
        assignment.setSowId(7L);
        assignment.setDesignationId(20L);
        assignment.setPositionType("BILLABLE");
        assignment.setLeadId(1L);
        assignment.setManagerId(2L);
        assignment.setAllocationPercentage(100);
        assignment.setIsPrimaryAssignment(true);
        assignment.setEffectiveFrom(LocalDate.of(2026, 8, 13));
        assignment.setStatus("ACTIVE");
        return assignment;
    }

    private SowAssignmentUpdateRequest updateRequest(
            String status, LocalDate endDate, boolean primary) {
        return SowAssignmentUpdateRequest.builder()
                .designationId(20L)
                .positionType("BILLABLE")
                .leadId(1L)
                .managerId(2L)
                .allocationPercentage(100)
                .isPrimaryAssignment(primary)
                .assignmentStartDate(LocalDate.of(2026, 8, 13))
                .assignmentEndDate(endDate)
                .assignmentStatus(status)
                .updatedBy(1L)
                .build();
    }

    private Employee activeEmployee(Long id, String firstName, String lastName) {
        Employee employee = employee(id, firstName, lastName);
        employee.setStatus("ACTIVE");
        return employee;
    }

    private Employee employee(Long id, String firstName, String lastName) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        return employee;
    }
}
