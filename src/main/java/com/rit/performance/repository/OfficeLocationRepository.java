package com.rit.performance.repository;
import com.rit.performance.entity.OfficeLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface OfficeLocationRepository extends JpaRepository<OfficeLocation,Long> {
 List<OfficeLocation> findAllByOrderByCountryCodeAscOfficeNameAscIdAsc();
}
