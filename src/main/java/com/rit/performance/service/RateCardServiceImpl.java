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

    @Override @Transactional
    public RateCardResponse create(RateCardRequest request) {
        validateRequest(request);
        RateCard rateCard = new RateCard();
        apply(rateCard, request);
        return toResponse(rateCardRepository.save(rateCard));
    }
    @Override @Transactional
    public RateCardResponse update(Long id, RateCardRequest request) {
        RateCard rateCard = find(id);

        // A change to a rate-defining field is a new effective-dated rate card,
        // not a mutation of the historical record.
        if (rateDefiningFieldsChanged(rateCard, request)) {
            validateRequest(request);
            RateCard successor = new RateCard();
            apply(successor, request);
            return toResponse(rateCardRepository.save(successor));
        }

        validateRequest(request);
        apply(rateCard, request);
        return toResponse(rateCardRepository.save(rateCard));
    }
    @Override public RateCardResponse getById(Long id) { return toResponse(find(id)); }
    @Override public List<RateCardResponse> getAll() {
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
        if (request.getSkill() == null || request.getSkill().isBlank()) {
            throw new InvalidOperationException("skill is required");
        }
        if (request.getSkill().trim().length() > 100) {
            throw new InvalidOperationException("skill must not exceed 100 characters");
        }
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
        if (request.getEffectiveTo() != null
                && request.getEffectiveTo().isBefore(request.getEffectiveFrom())) {
            throw new InvalidOperationException(
                    "effectiveTo cannot be before effectiveFrom");
        }
        if (Set.of("ACTIVE", "DRAFT").contains(normalizedStatus)
                && request.getEffectiveTo() != null) {
            throw new InvalidOperationException(
                    "effectiveTo must be null when status is ACTIVE or DRAFT");
        }
        if ("INACTIVE".equals(normalizedStatus) && request.getEffectiveTo() == null) {
            throw new InvalidOperationException(
                    "effectiveTo is required when status is INACTIVE");
        }
    }
    private void apply(RateCard rateCard, RateCardRequest request) {
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + request.getClientId()));
        rateCard.setPositionTitleId(request.getPositionTitleId());
        rateCard.setSkill(request.getSkill().trim());
        rateCard.setLocationId(request.getLocationId());
        rateCard.setSeniorityId(request.getSeniorityId());
        rateCard.setClient(client);
        rateCard.setHourlyRate(request.getHourlyRate());
        rateCard.setCurrency(request.getCurrency() == null || request.getCurrency().isBlank() ? "USD" : request.getCurrency().trim().toUpperCase(Locale.ROOT));
        rateCard.setEffectiveFrom(request.getEffectiveFrom());
        rateCard.setEffectiveTo(request.getEffectiveTo());
        rateCard.setStatus(status(request.getStatus()));
    }
    private boolean rateDefiningFieldsChanged(RateCard existing, RateCardRequest request) {
        return !Objects.equals(existing.getPositionTitleId(), request.getPositionTitleId())
                || !Objects.equals(existing.getSkill(), normalizedSkill(request.getSkill()))
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
    private String normalizedSkill(String value) {
        return value == null ? null : value.trim();
    }
    private RateCardResponse toResponse(RateCard r) {
        Client c = r.getClient();
        return RateCardResponse.builder().id(r.getId())
                .positionTitleId(r.getPositionTitleId())
                .positionTitleName(resolveLookupName(r.getPositionTitleId(), "DESIGNATION"))
                .skill(r.getSkill())
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
        if (!Set.of("DRAFT", "ACTIVE", "INACTIVE").contains(normalized)) throw new InvalidOperationException("status must be DRAFT, ACTIVE, or INACTIVE"); return normalized; }
    private String required(String value) { return value == null || value.isBlank() ? "" : value.trim(); }
}
