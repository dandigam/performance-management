package com.rit.performance.service;

import com.rit.performance.dto.FeatureRequest;
import com.rit.performance.entity.*;
import com.rit.performance.exception.*;
import com.rit.performance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeatureFlagService {
    private final FeatureRepository features;
    private final RoleFeatureFlagRepository flags;
    private final LookupValueRepository lookups;

    public record FeatureResponse(Long id, String code, String name) {}
    public record RoleFeatureResponse(Long roleId, Long featureId, String code, String name, boolean enabled) {}

    public List<FeatureResponse> list() {
        return features.findAllByOrderByCodeAsc().stream().map(this::response).toList();
    }

    public FeatureResponse get(Long id) { return response(requireFeature(id)); }

    @Transactional
    public FeatureResponse create(FeatureRequest request) {
        if (features.existsByCode(request.code())) throw new DuplicateResourceException("Feature code already exists");
        Feature feature = new Feature();
        feature.setCode(request.code());
        feature.setName(request.name().trim());
        return response(features.saveAndFlush(feature));
    }

    @Transactional
    public FeatureResponse update(Long id, FeatureRequest request) {
        Feature feature = requireFeature(id);
        if (features.existsByCodeAndIdNot(request.code(), id)) throw new DuplicateResourceException("Feature code already exists");
        feature.setCode(request.code());
        feature.setName(request.name().trim());
        return response(features.saveAndFlush(feature));
    }

    public List<RoleFeatureResponse> forRole(Long roleId) {
        requireRole(roleId);
        Map<Long, Boolean> enabled = new HashMap<>();
        flags.findByRoleId(roleId).forEach(flag -> enabled.put(flag.getFeature().getId(), flag.isEnabled()));
        return features.findAllByOrderByCodeAsc().stream().map(feature ->
                new RoleFeatureResponse(roleId, feature.getId(), feature.getCode(), feature.getName(),
                        enabled.getOrDefault(feature.getId(), false))).toList();
    }

    @Transactional
    public RoleFeatureResponse set(Long roleId, Long featureId, boolean enabled) {
        LookupValue role = requireRole(roleId);
        Feature feature = requireFeature(featureId);
        RoleFeatureFlag flag = flags.findByRoleIdAndFeatureId(roleId, featureId).orElseGet(RoleFeatureFlag::new);
        flag.setRole(role);
        flag.setFeature(feature);
        flag.setEnabled(enabled);
        flags.saveAndFlush(flag);
        return new RoleFeatureResponse(roleId, featureId, feature.getCode(), feature.getName(), enabled);
    }

    public Map<String, Boolean> forLogin(Long roleId) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        features.findAllByOrderByCodeAsc().forEach(feature -> result.put(feature.getCode(), false));
        if (roleId != null) flags.findByRoleId(roleId).forEach(flag -> result.put(flag.getFeature().getCode(), flag.isEnabled()));
        return result;
    }

    private Feature requireFeature(Long id) {
        return features.findById(id).orElseThrow(() -> new ResourceNotFoundException("Feature not found: " + id));
    }

    private LookupValue requireRole(Long id) {
        LookupValue role = lookups.findById(id).orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
        if (!"SYSTEM_ROLE".equalsIgnoreCase(role.getLookupType().getCode())
                || !role.isActive() || !role.getLookupType().isActive())
            throw new InvalidOperationException("roleId must reference an active SYSTEM_ROLE");
        return role;
    }

    private FeatureResponse response(Feature feature) {
        return new FeatureResponse(feature.getId(), feature.getCode(), feature.getName());
    }
}
