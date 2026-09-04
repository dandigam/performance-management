package com.rit.performance.repository;

import com.rit.performance.entity.LookupValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LookupValueRepository extends JpaRepository<LookupValue, Long> {
    List<LookupValue> findByLookupTypeCodeIgnoreCaseAndLookupTypeActiveTrueAndActiveTrueOrderByDisplayOrderAscIdAsc(
            String typeCode
    );

    boolean existsByLookupTypeIdAndCodeIgnoreCase(Long lookupTypeId, String code);

    Optional<LookupValue> findByLookupTypeIdAndCodeIgnoreCase(Long lookupTypeId, String code);

    boolean existsByLookupTypeIdAndCodeIgnoreCaseAndIdNot(
            Long lookupTypeId,
            String code,
            Long id
    );

    Optional<LookupValue> findByIdAndLookupTypeId(Long id, Long lookupTypeId);

    List<LookupValue> findByLookupTypeIdOrderByDisplayOrderAscIdAsc(Long lookupTypeId);

    List<LookupValue> findByLookupTypeIdAndActiveTrueOrderByDisplayOrderAscIdAsc(Long lookupTypeId);

    Optional<LookupValue> findByLookupTypeCodeIgnoreCaseAndDisplayOrderAndLookupTypeActiveTrueAndActiveTrue(
            String typeCode, int displayOrder);

    Optional<LookupValue> findByLookupTypeCodeIgnoreCaseAndCodeIgnoreCaseAndLookupTypeActiveTrueAndActiveTrue(
            String typeCode, String code);

}
