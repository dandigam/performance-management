package com.rit.performance.service;

import com.rit.performance.dto.request.SowInvoiceRequest;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.entity.Sow;
import com.rit.performance.entity.SowInvoice;
import com.rit.performance.entity.SowMilestone;
import com.rit.performance.exception.DuplicateResourceException;
import com.rit.performance.repository.SowInvoiceRepository;
import com.rit.performance.repository.SowMilestoneRepository;
import com.rit.performance.service.impl.SowInvoiceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SowInvoiceServiceImplTest {
    @Mock private SowInvoiceRepository invoiceRepository;
    @Mock private SowMilestoneRepository milestoneRepository;

    private SowInvoiceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SowInvoiceServiceImpl(invoiceRepository, milestoneRepository);
    }

    @Test
    void returnsExpectedValuesFromSowAndMilestone() {
        SowInvoice invoice = invoice();
        when(invoiceRepository.findAllWithDetails()).thenReturn(List.of(invoice));

        var response = service.getAll(null, null, null).get(0);

        assertEquals("Car Management", response.getDepartmentName());
        assertEquals("FMS", response.getSowName());
        assertEquals("UI Changes", response.getMilestoneName());
        assertEquals(LocalDate.of(2026, 7, 30), response.getExpectedCompletionDate());
        assertEquals(new BigDecimal("500.00"), response.getExpectedAmount());
        assertEquals("DRAFT", response.getInvoiceStatus());
        assertEquals("UNPAID", response.getPaymentStatus());
    }

    @Test
    void derivesPaidStatusFromReceivedAmount() {
        SowInvoice invoice = invoice();
        when(milestoneRepository.findById(10L)).thenReturn(Optional.of(invoice.getMilestone()));
        when(invoiceRepository.existsByMilestone_Id(10L)).thenReturn(false);
        when(invoiceRepository.save(any(SowInvoice.class))).thenAnswer(invocation -> {
            SowInvoice saved = invocation.getArgument(0);
            saved.setId(20L);
            return saved;
        });
        SowInvoiceRequest request = SowInvoiceRequest.builder()
                .milestoneId(10L)
                .actualInvoiceDate(LocalDate.of(2026, 7, 30))
                .invoiceAmount(new BigDecimal("500.00"))
                .invoiceStatus("submitted")
                .receivedAmount(new BigDecimal("500.00"))
                .paymentReceivedDate(LocalDate.of(2026, 8, 15))
                .updatedBy(3L)
                .build();

        var response = service.create(request);

        assertEquals("SUBMITTED", response.getInvoiceStatus());
        assertEquals("PAID", response.getPaymentStatus());
        assertEquals(20L, response.getId());
    }

    @Test
    void rejectsSecondInvoiceForSameMilestone() {
        SowInvoice invoice = invoice();
        when(milestoneRepository.findById(10L)).thenReturn(Optional.of(invoice.getMilestone()));
        when(invoiceRepository.existsByMilestone_Id(10L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> service.create(SowInvoiceRequest.builder().milestoneId(10L).build()));
    }

    @Test
    void createsOneDraftInvoiceForEachMissingMilestone() {
        SowInvoice existingInvoice = invoice();
        Sow sow = existingInvoice.getSow();
        SowMilestone secondMilestone = SowMilestone.builder()
                .id(11L).sow(sow).milestoneName("API Changes").build();
        when(invoiceRepository.findByMilestone_IdIn(List.of(10L, 11L)))
                .thenReturn(List.of(existingInvoice));

        service.createDraftInvoices(sow, List.of(existingInvoice.getMilestone(), secondMilestone));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SowInvoice>> captor = ArgumentCaptor.forClass(List.class);
        verify(invoiceRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(11L, captor.getValue().get(0).getMilestone().getId());
        assertEquals("DRAFT", captor.getValue().get(0).getInvoiceStatus());
        assertEquals("UNPAID", captor.getValue().get(0).getPaymentStatus());
    }

    private SowInvoice invoice() {
        LookupValue department = LookupValue.builder().id(48L).name("Car Management").build();
        Sow sow = Sow.builder().id(7L).sowCode("SW001").sowName("FMS")
                .businessUnit(department).build();
        SowMilestone milestone = SowMilestone.builder()
                .id(10L).sow(sow).milestoneName("UI Changes")
                .endDate(LocalDate.of(2026, 7, 30))
                .invoiceDate(LocalDate.of(2026, 7, 30))
                .amount(new BigDecimal("500.00"))
                .build();
        return SowInvoice.builder().id(20L).sow(sow).milestone(milestone)
                .invoiceStatus("DRAFT").paymentStatus("UNPAID").build();
    }
}
