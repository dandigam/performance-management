package com.rit.performance.service;

import com.rit.performance.dto.RateCardRequest;
import com.rit.performance.dto.RateCardResponse;
import com.rit.performance.entity.Client;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.entity.RateCard;
import com.rit.performance.exception.InvalidOperationException;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.repository.ClientRepository;
import com.rit.performance.repository.LookupValueRepository;
import com.rit.performance.repository.RateCardRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RateCardServiceImpl implements RateCardService {
    private final RateCardRepository rateCardRepository;
    private final ClientRepository clientRepository;
    private final LookupValueRepository lookupValueRepository;
    private final RateCardNotificationService notifications;

    @Override @Transactional
    public RateCardResponse create(RateCardRequest request) {
        validateRequest(request);
        lockClient(request);
        rejectDuplicate(request, null);
        RateCard rateCard = new RateCard();
        apply(rateCard, request);
        RateCardResponse response = toResponse(rateCardRepository.saveAndFlush(rateCard));
        notifications.changed(response, null, "CREATED");
        return response;
    }
    @Override @Transactional
    public RateCardResponse update(Long id, RateCardRequest request) {
        validateRequest(request);
        lockClient(request);
        RateCard rateCard = find(id);
        RateCardResponse previous = toResponse(rateCard);

        // A change to a rate-defining field is a new effective-dated rate card,
        // not a mutation of the historical record.
        if (rateDefiningFieldsChanged(rateCard, request)) {
            rejectDuplicate(request, null);
            RateCard successor = new RateCard();
            apply(successor, request);
            RateCardResponse response = toResponse(rateCardRepository.saveAndFlush(successor));
            notifications.changed(response, previous, "NEW_RATE");
            return response;
        }

        rejectDuplicate(request, id);
        boolean changed = !Objects.equals(rateCard.getEffectiveTo(), request.getEffectiveTo())
                || !Objects.equals(rateCard.getStatus(), status(request.getStatus()));
        apply(rateCard, request);
        RateCardResponse response = toResponse(rateCardRepository.saveAndFlush(rateCard));
        if (changed) notifications.changed(response, previous,
                "INACTIVE".equals(response.getStatus()) && !"INACTIVE".equals(previous.getStatus())
                        ? "RETIRED" : "UPDATED");
        return response;
    }
    // End dates are inclusive. Use the server's local date consistently for saves and expiry.
    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "${app.rate-cards.expiry-delay-ms:60000}")
    @Transactional
    public void expireRateCards() {
        rateCardRepository.expireActiveCards(LocalDate.now());
    }

    @Override @Transactional public RateCardResponse getById(Long id) {
        expireRateCards();
        return toResponse(find(id));
    }
    @Override @Transactional public List<RateCardResponse> getAll() {
        expireRateCards();
        return rateCardRepository.findAllWithDetails().stream()
                .sorted(Comparator.comparing(
                                RateCard::getEffectiveFrom,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(
                                RateCard::getCreatedOn,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(
                                RateCard::getId,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }
    @Override @Transactional public void delete(Long id) { rateCardRepository.delete(find(id)); }

    private RateCard find(Long id) { return rateCardRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Rate card not found: " + id)); }
    private void validateRequest(RateCardRequest request) {
        lookupCode(request.getPositionTitleId(), "DESIGNATION", "positionTitleId");
        lookupCode(request.getMainSkillId(), "SKILL", "mainSkillId");
        if (request.getLocationId() != null) {
            lookupCode(request.getLocationId(), "LOCATION", "locationId");
        }
        if (request.getSeniorityId() != null) {
            lookupCode(request.getSeniorityId(), "SENIORITY", "seniorityId");
        }
        if (request.getEffectiveFrom() == null) {
            throw new InvalidOperationException("effectiveFrom is required");
        }
        String normalizedStatus = status(request.getStatus());
        if ("EXPIRED".equals(normalizedStatus)
                && (request.getEffectiveTo() == null || !request.getEffectiveTo().isBefore(LocalDate.now()))) {
            throw new InvalidOperationException("EXPIRED requires an effectiveTo date before today");
        }
        if (request.getEffectiveTo() != null
                && request.getEffectiveTo().isBefore(request.getEffectiveFrom())) {
            throw new InvalidOperationException(
                    "effectiveTo cannot be before effectiveFrom");
        }
        if ("INACTIVE".equals(normalizedStatus) && request.getEffectiveTo() == null) {
            throw new InvalidOperationException(
                    "effectiveTo is required when status is INACTIVE");
        }
    }
    private void lockClient(RateCardRequest request) {
        if (request.getClientId() == null || request.getClientId() <= 0)
            throw new InvalidOperationException("A positive clientId is required");
        // Serialize writes even when no rate card exists yet for this client.
        clientRepository.findForRateCardUpdate(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + request.getClientId()));
    }

    private void rejectDuplicate(RateCardRequest request, Long excludedId) {
        RateCard duplicate = rateCardRepository.findExactDuplicates(
                request.getClientId(), request.getEffectiveFrom(), request.getPositionTitleId(),
                request.getMainSkillId(), request.getLocationId(), request.getSeniorityId(),
                request.getHourlyRate())
                .stream().filter(existing -> !Objects.equals(existing.getId(), excludedId))
                .filter(existing -> Objects.equals(existing.getEffectiveTo(), request.getEffectiveTo()))
                .filter(existing -> Objects.equals(normalizedCurrency(existing.getCurrency()),
                        normalizedCurrency(request.getCurrency())))
                .filter(existing -> Objects.equals(trimToNull(existing.getAdditionalSkills()),
                        trimToNull(request.getAdditionalSkills())))
                .filter(existing -> Objects.equals(effectiveStatus(existing.getStatus(), existing.getEffectiveTo()),
                        effectiveStatus(request.getStatus(), request.getEffectiveTo())))
                .findFirst().orElse(null);
        if (duplicate != null) {
            String position = resolveLookupName(duplicate.getPositionTitleId(), "DESIGNATION");
            if (position == null || position.isBlank()) position = "Position #" + duplicate.getPositionTitleId();
            throw new com.rit.performance.exception.DuplicateResourceException(
                    "Rate card #" + duplicate.getId() + " already exists for " + position
                            + " with the same client, main skill, additional skills, location, seniority,"
                            + " effective start and end dates, hourly rate, currency and status."
                            + " Please review the existing rate card.");
        }
    }
    private void apply(RateCard rateCard, RateCardRequest request) {
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + request.getClientId()));
        rateCard.setPositionTitleId(request.getPositionTitleId());
        rateCard.setMainSkill(lookupValueRepository.findById(request.getMainSkillId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "mainSkillId lookup not found: " + request.getMainSkillId())));
        rateCard.setAdditionalSkills(trimToNull(request.getAdditionalSkills()));
        rateCard.setLocationId(request.getLocationId());
        rateCard.setSeniorityId(request.getSeniorityId());
        rateCard.setClient(client);
        rateCard.setHourlyRate(request.getHourlyRate());
        rateCard.setCurrency(request.getCurrency() == null || request.getCurrency().isBlank() ? "USD" : request.getCurrency().trim().toUpperCase(Locale.ROOT));
        rateCard.setEffectiveFrom(request.getEffectiveFrom());
        rateCard.setEffectiveTo(request.getEffectiveTo());
        rateCard.setStatus(effectiveStatus(request.getStatus(), request.getEffectiveTo()));
    }
    private String effectiveStatus(String value, LocalDate effectiveTo) {
        String requestedStatus = status(value);
        return "ACTIVE".equals(requestedStatus) && effectiveTo != null
                && effectiveTo.isBefore(LocalDate.now()) ? "EXPIRED" : requestedStatus;
    }
    private boolean rateDefiningFieldsChanged(RateCard existing, RateCardRequest request) {
        return !Objects.equals(existing.getPositionTitleId(), request.getPositionTitleId())
                || existing.getMainSkill() == null
                || !Objects.equals(existing.getMainSkill().getId(), request.getMainSkillId())
                || !Objects.equals(existing.getAdditionalSkills(),
                        trimToNull(request.getAdditionalSkills()))
                || !Objects.equals(existing.getLocationId(), request.getLocationId())
                || !Objects.equals(existing.getSeniorityId(), request.getSeniorityId())
                || existing.getClient() == null || !Objects.equals(existing.getClient().getId(), request.getClientId())
                || !sameAmount(existing.getHourlyRate(), request.getHourlyRate())
                || !Objects.equals(existing.getCurrency(), normalizedCurrency(request.getCurrency()))
                || !Objects.equals(existing.getEffectiveFrom(), request.getEffectiveFrom());
    }
    private boolean sameAmount(BigDecimal first, BigDecimal second) {
        return first == null ? second == null : second != null && first.compareTo(second) == 0;
    }
    private String normalizedCurrency(String value) {
        return value == null || value.isBlank() ? "USD" : value.trim().toUpperCase(Locale.ROOT);
    }
    private RateCardResponse toResponse(RateCard r) {
        Client c = r.getClient();
        return RateCardResponse.builder().id(r.getId())
                .positionTitleId(r.getPositionTitleId())
                .positionTitleName(resolveLookupName(r.getPositionTitleId(), "DESIGNATION"))
                .mainSkillId(r.getMainSkill() == null ? null : r.getMainSkill().getId())
                .mainSkillName(r.getMainSkill() == null ? null : r.getMainSkill().getName())
                .additionalSkills(r.getAdditionalSkills())
                .locationId(r.getLocationId())
                .locationName(resolveLookupName(r.getLocationId(), "LOCATION"))
                .seniorityId(r.getSeniorityId())
                .seniorityName(resolveLookupName(r.getSeniorityId(), "SENIORITY"))
                .clientId(c.getId())
                .clientName(c.getClientName())
                .hourlyRate(r.getHourlyRate())
                .currency(r.getCurrency())
                .effectiveFrom(r.getEffectiveFrom())
                .effectiveTo(r.getEffectiveTo())
                .status(r.getStatus())
                .createdDate(r.getCreatedOn())
                .build();
    }
    private String resolveLookupName(Long lookupId, String lookupTypeCode) {
        if (lookupId == null) return null;
        return lookupValueRepository.findById(lookupId)
                .filter(value -> value.getLookupType() != null && lookupTypeCode.equalsIgnoreCase(value.getLookupType().getCode()))
                .map(value -> value.getName() == null || value.getName().isBlank() ? value.getCode() : value.getName())
                .orElse(null);
    }
    private String lookupCode(Long lookupId, String lookupTypeCode, String fieldName) {
        if (lookupId == null) {
            throw new InvalidOperationException(fieldName + " lookup id is required");
        }
        LookupValue lookup = lookupValueRepository.findById(lookupId)
                .orElseThrow(() -> new ResourceNotFoundException(fieldName + " lookup not found: " + lookupId));
        if (lookup.getLookupType() == null || !lookupTypeCode.equalsIgnoreCase(lookup.getLookupType().getCode())) {
            throw new InvalidOperationException(fieldName + " lookup id must belong to the " + lookupTypeCode + " lookup group");
        }
        return (lookup.getCode() == null || lookup.getCode().isBlank() ? lookup.getName() : lookup.getCode())
                .trim().toUpperCase(Locale.ROOT);
    }
    private String status(String value) { String normalized = value == null || value.isBlank() ? "ACTIVE" : value.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("DRAFT", "ACTIVE", "INACTIVE", "EXPIRED").contains(normalized)) throw new InvalidOperationException("status must be DRAFT, ACTIVE, INACTIVE, or EXPIRED"); return normalized; }
    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
