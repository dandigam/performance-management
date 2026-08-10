package com.rit.performance.repository;

import com.rit.performance.entity.Vendor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
    boolean existsByVendorCodeIgnoreCase(String vendorCode);
    boolean existsByVendorCodeIgnoreCaseAndIdNot(String vendorCode, Long id);

    @Override
    @EntityGraph(attributePaths = "documents")
    List<Vendor> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = "documents")
    Optional<Vendor> findById(Long id);
}
