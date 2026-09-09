package com.rit.performance.service.impl;

import com.rit.performance.dto.request.SowSignatureUpdateRequest;
import com.rit.performance.dto.request.SowStatusUpdateRequest;
import com.rit.performance.dto.response.SowResponse;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.entity.Sow;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.repository.*;
import com.rit.performance.service.SowInvoiceService;
import com.rit.performance.service.SowResourceRequirementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SowServiceImplTest {
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

    @InjectMocks private SowServiceImpl service;

    @Test
    void updateStatusAppliesValidTransitionAndEffectiveDate() {
        LocalDate effectiveDate = LocalDate.now().minusDays(1);
        Sow sow = sowWithStatus("ACTIVE", LocalDate.now().minusMonths(1));
        LookupValue completed = status("COMPLETED");
        when(sowRepository.findByIdWithDetails(20L)).thenReturn(Optional.of(sow));
        when(lookupValueRepository
                .findByLookupTypeCodeIgnoreCaseAndCodeIgnoreCaseAndLookupTypeActiveTrueAndActiveTrue(
                        "SOW_STATUS", "COMPLETED"))
                .thenReturn(Optional.of(completed));
        when(sowRepository.save(any(Sow.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(positionAssignmentRepository
                .findByMilestonePosition_Sow_IdAndStatusIgnoreCase(20L, "ACTIVE"))
                .thenReturn(List.of());

        SowResponse response = service.updateStatus(20L, SowStatusUpdateRequest.builder()
                .status("COMPLETED")
                .statusEffectiveDate(effectiveDate)
                .build());

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getStatusEffectiveDate()).isEqualTo(effectiveDate);
    }

    @Test
    void updateStatusRejectsInvalidTransition() {
        Sow sow = sowWithStatus("ACTIVE", LocalDate.now().minusMonths(1));
        when(sowRepository.findByIdWithDetails(20L)).thenReturn(Optional.of(sow));
        when(lookupValueRepository
                .findByLookupTypeCodeIgnoreCaseAndCodeIgnoreCaseAndLookupTypeActiveTrueAndActiveTrue(
                        "SOW_STATUS", "WAITING_FOR_APPROVAL"))
                .thenReturn(Optional.of(status("WAITING_FOR_APPROVAL")));

        assertThatThrownBy(() -> service.updateStatus(20L, SowStatusUpdateRequest.builder()
                .status("WAITING_FOR_APPROVAL")
                .statusEffectiveDate(LocalDate.now())
                .build()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("Invalid SOW status transition: ACTIVE -> WAITING_FOR_APPROVAL");
    }

    @Test
    void waitingForApprovalAllowsEffectiveDateBeforeSowStartDate() {
        LocalDate effectiveDate = LocalDate.now().minusDays(1);
        Sow sow = sowWithStatus("DRAFT", LocalDate.now());
        sow.setStartDate(LocalDate.now().plusMonths(1));
        when(sowRepository.findByIdWithDetails(20L)).thenReturn(Optional.of(sow));
        when(lookupValueRepository
                .findByLookupTypeCodeIgnoreCaseAndCodeIgnoreCaseAndLookupTypeActiveTrueAndActiveTrue(
                        "SOW_STATUS", "WAITING_FOR_APPROVAL"))
                .thenReturn(Optional.of(status("WAITING_FOR_APPROVAL")));
        when(sowRepository.save(any(Sow.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(positionAssignmentRepository
                .findByMilestonePosition_Sow_IdAndStatusIgnoreCase(20L, "ACTIVE"))
                .thenReturn(List.of());

        SowResponse response = service.updateStatus(20L, SowStatusUpdateRequest.builder()
                .status("WAITING_FOR_APPROVAL")
                .statusEffectiveDate(effectiveDate)
                .build());

        assertThat(response.getStatus()).isEqualTo("WAITING_FOR_APPROVAL");
        assertThat(response.getStatusEffectiveDate()).isEqualTo(effectiveDate);
    }

    @Test
    void updateStatusAllowsWaitingForApprovalToApproved() {
        LocalDate effectiveDate = LocalDate.now();
        Sow sow = sowWithStatus("WAITING_FOR_APPROVAL", LocalDate.now().minusDays(1));
        when(sowRepository.findByIdWithDetails(20L)).thenReturn(Optional.of(sow));
        when(lookupValueRepository
                .findByLookupTypeCodeIgnoreCaseAndCodeIgnoreCaseAndLookupTypeActiveTrueAndActiveTrue(
                        "SOW_STATUS", "APPROVED"))
                .thenReturn(Optional.of(status("APPROVED")));
        when(sowRepository.save(any(Sow.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(positionAssignmentRepository
                .findByMilestonePosition_Sow_IdAndStatusIgnoreCase(20L, "ACTIVE"))
                .thenReturn(List.of());

        SowResponse response = service.updateStatus(20L, SowStatusUpdateRequest.builder()
                .status("APPROVED")
                .statusEffectiveDate(effectiveDate)
                .build());

        assertThat(response.getStatus()).isEqualTo("APPROVED");
        assertThat(response.getStatusEffectiveDate()).isEqualTo(effectiveDate);
    }

    @Test
    void updateSignatureClearsDateWhenUnsigned() {
        Sow sow = sowWithStatus("ACTIVE", LocalDate.now().minusMonths(1));
        sow.setSignedStatus("SIGNED");
        sow.setSignedDate(LocalDate.now().minusDays(2));
        when(sowRepository.findByIdWithDetails(20L)).thenReturn(Optional.of(sow));
        when(sowRepository.save(any(Sow.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(positionAssignmentRepository
                .findByMilestonePosition_Sow_IdAndStatusIgnoreCase(20L, "ACTIVE"))
                .thenReturn(List.of());

        SowResponse response = service.updateSignature(20L,
                SowSignatureUpdateRequest.builder().signedStatus("UNSIGNED").build());

        assertThat(response.getSignedStatus()).isEqualTo("UNSIGNED");
        assertThat(response.getSignedDate()).isNull();
    }

    @Test
    void updateSignatureAllowsSignedDateBeforeSowStartDate() {
        LocalDate signedDate = LocalDate.now().minusDays(1);
        Sow sow = sowWithStatus("DRAFT", LocalDate.now().minusMonths(1));
        sow.setStartDate(LocalDate.now().plusMonths(1));
        when(sowRepository.findByIdWithDetails(20L)).thenReturn(Optional.of(sow));
        when(sowRepository.save(any(Sow.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(positionAssignmentRepository
                .findByMilestonePosition_Sow_IdAndStatusIgnoreCase(20L, "ACTIVE"))
                .thenReturn(List.of());

        SowResponse response = service.updateSignature(20L,
                SowSignatureUpdateRequest.builder()
                        .signedStatus("SIGNED")
                        .signedDate(signedDate)
                        .build());

        assertThat(response.getSignedStatus()).isEqualTo("SIGNED");
        assertThat(response.getSignedDate()).isEqualTo(signedDate);
    }

    @Test
    void updateSignatureRejectsFutureSignedDate() {
        Sow sow = sowWithStatus("DRAFT", LocalDate.now().minusMonths(1));
        sow.setStartDate(LocalDate.now().plusMonths(1));
        when(sowRepository.findByIdWithDetails(20L)).thenReturn(Optional.of(sow));

        assertThatThrownBy(() -> service.updateSignature(20L,
                SowSignatureUpdateRequest.builder()
                        .signedStatus("SIGNED")
                        .signedDate(LocalDate.now().plusDays(1))
                        .build()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessage("signedDate cannot be in the future");
    }

    private Sow sowWithStatus(String code, LocalDate statusEffectiveDate) {
        Sow sow = new Sow();
        sow.setId(20L);
        sow.setSowName("Test SOW");
        sow.setStartDate(LocalDate.now().minusMonths(2));
        sow.setStatus(status(code));
        sow.setStatusEffectiveDate(statusEffectiveDate);
        return sow;
    }

    private LookupValue status(String code) {
        LookupValue status = new LookupValue();
        status.setCode(code);
        status.setName(code);
        status.setActive(true);
        return status;
    }
}
