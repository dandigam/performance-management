package com.rit.performance.repository;

import com.rit.performance.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @org.springframework.data.jpa.repository.Query("select t.user.id from RefreshToken t where t.tokenHash = :hash")
    Optional<Long> findUserIdByHash(@org.springframework.data.repository.query.Param("hash") String hash);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("update RefreshToken t set t.revokedAt = :now where t.user.id = :userId and t.revokedAt is null")
    int revokeAllForUser(@org.springframework.data.repository.query.Param("userId") Long userId,
                        @org.springframework.data.repository.query.Param("now") java.time.Instant now);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
