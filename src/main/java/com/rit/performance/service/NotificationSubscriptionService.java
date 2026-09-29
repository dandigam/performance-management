package com.rit.performance.service;

import com.rit.performance.dto.NotificationSubscriptionRequest;
import com.rit.performance.dto.NotificationSubscriptionResponse;
import com.rit.performance.entity.NotificationSubscription;
import com.rit.performance.exception.*;
import com.rit.performance.repository.*;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationSubscriptionService {
    public static final String CATEGORY_TYPE = "NOTIFICATION_CATEGORY";
    private final NotificationSubscriptionRepository repository;
    private final LookupValueRepository lookupValues;

    public List<NotificationSubscriptionResponse> list() {
        return repository.findAllByOrderByIdAsc().stream().map(this::response).toList();
    }

    public NotificationSubscriptionResponse get(Long id) {
        return response(find(id));
    }

    @Transactional
    public NotificationSubscriptionResponse create(NotificationSubscriptionRequest request) {
        return save(new NotificationSubscription(), request);
    }

    @Transactional
    public NotificationSubscriptionResponse update(Long id, NotificationSubscriptionRequest request) {
        return save(find(id), request);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private NotificationSubscription find(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Notification subscription not found"));
    }

    private NotificationSubscriptionResponse save(NotificationSubscription entity,
                                                  NotificationSubscriptionRequest request) {
        var category = lookupValues.findById(request.categoryId()).orElseThrow(() ->
                new ResourceNotFoundException("Notification category not found"));
        if (!CATEGORY_TYPE.equals(category.getLookupType().getCode())) {
            throw new InvalidOperationException("Category must belong to NOTIFICATION_CATEGORY");
        }
        if (Boolean.TRUE.equals(request.active())
                && (!category.isActive() || !category.getLookupType().isActive())) {
            throw new InvalidOperationException("An active subscription requires an active category");
        }
        boolean duplicate = entity.getId() == null
                ? repository.existsByCategoryId(category.getId())
                : repository.existsByCategoryIdAndIdNot(category.getId(), entity.getId());
        if (duplicate) {
            throw new DuplicateResourceException("A subscription already exists for this category");
        }
        String emails = normalizeEmails(request.emailAddresses());
        entity.setCategory(category);
        entity.setEmailAddresses(emails);
        entity.setRecipientType(request.recipientType());
        entity.setActive(request.active());
        return response(repository.saveAndFlush(entity));
    }

    static String normalizeEmails(String input) {
        if (input == null || input.isBlank() || input.length() > 10000
                || input.contains("\r") || input.contains("\n")) {
            throw new InvalidOperationException("Provide a comma-separated email list of at most 10000 characters");
        }
        Map<String, String> addresses = new LinkedHashMap<>();
        for (String part : input.split(",", -1)) {
            String email = part.trim();
            try {
                InternetAddress address = new InternetAddress(email, true);
                address.validate();
                if (email.length() > 254 || address.isGroup() || address.getPersonal() != null
                        || !email.equals(address.getAddress())) {
                    throw new AddressException();
                }
            } catch (AddressException exception) {
                throw new InvalidOperationException("Each entry must be a valid plain email address");
            }
            addresses.putIfAbsent(email.toLowerCase(Locale.ROOT), email);
        }
        return String.join(",", addresses.values());
    }

    private NotificationSubscriptionResponse response(NotificationSubscription entity) {
        var category = entity.getCategory();
        return new NotificationSubscriptionResponse(entity.getId(), category.getId(), category.getCode(),
                category.getName(), entity.getEmailAddresses(), entity.getRecipientType(), entity.isActive(),
                entity.getCreatedBy(), entity.getCreatedOn(), entity.getUpdatedBy(), entity.getUpdatedOn());
    }
}
