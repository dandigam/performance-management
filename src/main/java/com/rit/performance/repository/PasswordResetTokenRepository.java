package com.rit.performance.repository;

import com.rit.performance.entity.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    @Query("select t.user.id from PasswordResetToken t where t.tokenHash = :hash")
    Optional<Long> findUserIdByHash(@Param("hash") String hash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true)
    @Query("update PasswordResetToken t set t.used = true where t.user.id = :userId and t.used = false")
    int invalidateAllForUser(@Param("userId") Long userId);
}
