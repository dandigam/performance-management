package com.rit.performance.repository;
import com.rit.performance.entity.CompanySettings;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface CompanySettingsRepository extends JpaRepository<CompanySettings,Long> {
 @Modifying
 @Query(value="INSERT IGNORE INTO company_settings (id, portal_name, created_on, updated_on) VALUES (1, 'RailInfo Tech', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",nativeQuery=true)
 void ensureSingleton();
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select s from CompanySettings s where s.id=1")
 Optional<CompanySettings> lockSingleton();
}
