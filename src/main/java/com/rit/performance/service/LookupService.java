package com.rit.performance.service;

import com.rit.performance.dto.LookupValueResponse;
import com.rit.performance.dto.LookupTypeResponse;
import com.rit.performance.dto.LookupTypeSummaryResponse;
import com.rit.performance.dto.LookupValueMutationResponse;
import com.rit.performance.dto.LookupValueUpsertRequest;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.repository.LookupTypeRepository;
import com.rit.performance.repository.LookupValueRepository;
import com.rit.performance.exception.ResourceNotFoundException;
import com.rit.performance.exception.DuplicateResourceException;
import com.rit.performance.exception.InvalidOperationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class LookupService {

    private final LookupTypeRepository typeRepository;
    private final LookupValueRepository valueRepository;

    public LookupService(LookupTypeRepository typeRepository, LookupValueRepository valueRepository) {
        this.typeRepository = typeRepository;
        this.valueRepository = valueRepository;
    }

    public List<LookupTypeResponse> getAllLookups() {
        return typeRepository.findByActiveTrueOrderByIdAsc().stream()
                .map(type -> new LookupTypeResponse(
                        type.getId(),
                        type.getCode(),
                        type.getName(),
                        type.getDescription(),
                        valueRepository.findByLookupTypeIdAndActiveTrueOrderByDisplayOrderAscIdAsc(type.getId())
                                .stream()
                                .map(this::toValueResponse)
                                .toList()))
                .toList();
    }

    public List<LookupTypeSummaryResponse> getAllLookupTypes() {
        return typeRepository.findAllByOrderByIdAsc().stream()
                .map(type -> LookupTypeSummaryResponse.builder()
                        .id(type.getId())
                        .code(type.getCode())
                        .name(type.getName())
                        .description(type.getDescription())
                        .active(type.isActive())
                        .createdDate(type.getCreatedDate())
                        .build())
                .toList();
    }

    public List<LookupValueResponse> getLookupValues(Long typeId) {
        if (!typeRepository.existsById(typeId)) {
            throw new ResourceNotFoundException("Lookup type not found: " + typeId);
        }
        return valueRepository.findByLookupTypeIdOrderByDisplayOrderAscIdAsc(typeId)
                .stream()
                .map(this::toValueResponse)
                .toList();
    }

    @Transactional
    public LookupValueMutationResponse createLookupValue(
            Long typeId,
            LookupValueUpsertRequest request
    ) {
        if (request.getId() != null) {
            throw new InvalidOperationException("id must not be provided when creating a lookup value");
        }
        var type = typeRepository.findById(typeId)
                .orElseThrow(() -> new ResourceNotFoundException("Lookup type not found: " + typeId));
        String code = normalizeCode(request.getCode());
        if (valueRepository.existsByLookupTypeIdAndCodeIgnoreCase(typeId, code)) {
            throw new DuplicateResourceException(
                    "Lookup value code already exists for type " + typeId + ": " + code);
        }

        LookupValue value = new LookupValue();
        value.setLookupType(type);
        value.setCode(code);
        value.setName(request.getName().trim());
        value.setDisplayOrder(1);
        value.setActive(resolveActive(request, true));
        return toMutationResponse(valueRepository.save(value));
    }

    @Transactional
    public LookupValueMutationResponse updateLookupValue(
            Long typeId,
            LookupValueUpsertRequest request
    ) {
        if (!typeRepository.existsById(typeId)) {
            throw new ResourceNotFoundException("Lookup type not found: " + typeId);
        }
        if (request.getId() == null) {
            throw new InvalidOperationException("id is required when updating a lookup value");
        }
        LookupValue value = valueRepository.findByIdAndLookupTypeId(request.getId(), typeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lookup value " + request.getId() + " not found for type " + typeId));
        String code = normalizeCode(request.getCode());
        if (valueRepository.existsByLookupTypeIdAndCodeIgnoreCaseAndIdNot(
                typeId, code, value.getId())) {
            throw new DuplicateResourceException(
                    "Lookup value code already exists for type " + typeId + ": " + code);
        }

        value.setCode(code);
        value.setName(request.getName().trim());
        value.setActive(resolveActive(request, value.isActive()));
        return toMutationResponse(valueRepository.save(value));
    }

    private boolean resolveActive(LookupValueUpsertRequest request, boolean defaultValue) {
        Boolean statusActive = null;
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            String status = request.getStatus().trim().toUpperCase(Locale.ROOT);
            if ("ACTIVE".equals(status)) statusActive = true;
            else if ("INACTIVE".equals(status)) statusActive = false;
            else throw new InvalidOperationException("status must be ACTIVE or INACTIVE");
        }
        if (request.getActive() != null && statusActive != null
                && !request.getActive().equals(statusActive)) {
            throw new InvalidOperationException("status and active must represent the same state");
        }
        if (request.getActive() != null) return request.getActive();
        if (statusActive != null) return statusActive;
        return defaultValue;
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private LookupValueMutationResponse toMutationResponse(LookupValue value) {
        return LookupValueMutationResponse.builder()
                .id(value.getId())
                .lookupTypeId(value.getLookupType().getId())
                .code(value.getCode())
                .name(value.getName())
                .status(value.isActive() ? "ACTIVE" : "INACTIVE")
                .active(value.isActive())
                .build();
    }

    private LookupValueResponse toValueResponse(LookupValue value) {
        return new LookupValueResponse(
                value.getId(),
                value.getCode(),
                value.getName(),
                value.getDescription(),
                value.getDisplayOrder(),
                value.isActive() ? "ACTIVE" : "INACTIVE",
                value.isActive());
    }
}
