package com.rit.performance.repository;

import com.rit.performance.entity.UserNotification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.time.Instant;
import java.util.Optional;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    @Query("select n from UserNotification n where n.recipient.id = :userId "
            + "and (:unreadOnly = false or n.readAt is null) "
            + "and (:category is null or n.category = :category)")
    Page<UserNotification> inbox(@Param("userId") Long userId, @Param("unreadOnly") boolean unreadOnly,
                                 @Param("category") String category, Pageable pageable);
    long countByRecipientIdAndReadAtIsNull(Long userId);
    Optional<UserNotification> findByIdAndRecipientId(Long id, Long userId);
    // A locking read sees the latest committed row under MySQL REPEATABLE READ.
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from UserNotification n where n.recipient.id = :userId and n.deduplicationKey = :key")
    Optional<UserNotification> findForEvent(@Param("userId") Long userId, @Param("key") String key);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update UserNotification n set n.readAt = :now where n.id = :id "
            + "and n.recipient.id = :userId and n.readAt is null")
    int markRead(@Param("id") Long id, @Param("userId") Long userId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update UserNotification n set n.readAt = :now where n.recipient.id = :userId and n.readAt is null")
    int markAllRead(@Param("userId") Long userId, @Param("now") Instant now);
}
