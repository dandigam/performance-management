package com.rit.performance.service;

import com.rit.performance.dto.NotificationSubscriptionRequest;
import com.rit.performance.entity.*;
import com.rit.performance.entity.NotificationSubscription.RecipientType;
import com.rit.performance.exception.*;
import com.rit.performance.repository.*;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class NotificationSubscriptionServiceTest {
    private final NotificationSubscriptionRepository repository = mock(NotificationSubscriptionRepository.class);
    private final LookupValueRepository lookups = mock(LookupValueRepository.class);
    private final NotificationSubscriptionService service = new NotificationSubscriptionService(repository, lookups);

    private LookupValue category() {
        var type = new LookupType();
        type.setCode("NOTIFICATION_CATEGORY");
        var category = new LookupValue();
        category.setId(10L);
        category.setCode("ONBOARDING");
        category.setName("Onboarding");
        category.setLookupType(type);
        when(lookups.findById(10L)).thenReturn(Optional.of(category));
        when(repository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        return category;
    }

    @Test void trimsAndDeduplicatesIndividualAndDlAddresses() {
        assertThat(NotificationSubscriptionService.normalizeEmails(
                " HR@example.com,hr@example.com, finance-dl@example.com "))
                .isEqualTo("HR@example.com,finance-dl@example.com");
    }

    @Test void rejectsMalformedListsAndHeaderInjection() {
        for (String input : new String[]{"", "bad", "hr@example.com,", ",hr@example.com",
                "hr@example.com,,admin@example.com", "HR <hr@example.com>",
                "hr@example.com\r\nBcc: other@example.com", "team:hr@example.com;",
                "a".repeat(10001)}) {
            assertThatThrownBy(() -> NotificationSubscriptionService.normalizeEmails(input))
                    .as(input).isInstanceOf(InvalidOperationException.class);
        }
    }

    @Test void createsAndUpdatesCopyModeAndStatus() {
        category();
        var created = service.create(new NotificationSubscriptionRequest(10L,
                "hr@example.com, HR@example.com", RecipientType.BCC, true));
        assertThat(created.emailAddresses()).isEqualTo("hr@example.com");
        assertThat(created.recipientType()).isEqualTo(RecipientType.BCC);
        var existing = new NotificationSubscription();
        existing.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        var updated = service.update(1L, new NotificationSubscriptionRequest(10L,
                "admin@example.com", RecipientType.CC, false));
        assertThat(updated.active()).isFalse();
        assertThat(updated.recipientType()).isEqualTo(RecipientType.CC);
        verify(repository).existsByCategoryIdAndIdNot(10L, 1L);
        service.delete(1L);
        verify(repository).delete(existing);
    }

    @Test void rejectsDuplicateAndWrongCategoryWithoutSaving() {
        var category = category();
        var request = new NotificationSubscriptionRequest(10L, "hr@example.com", RecipientType.CC, true);
        when(repository.existsByCategoryId(10L)).thenReturn(true);
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(DuplicateResourceException.class);
        category.getLookupType().setCode("WORK_MODE");
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(InvalidOperationException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test void inactiveCategoryCanBeDisabledButNotActivated() {
        category().setActive(false);
        assertThatThrownBy(() -> service.create(new NotificationSubscriptionRequest(10L,
                "hr@example.com", RecipientType.BCC, true))).isInstanceOf(InvalidOperationException.class);
        assertThat(service.create(new NotificationSubscriptionRequest(10L,
                "hr@example.com", RecipientType.BCC, false)).active()).isFalse();
    }

    @Test void missingSubscriptionReturnsNotFound() {
        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
