package com.rit.performance.repository;

import com.rit.performance.entity.EmailNotification;
import com.rit.performance.service.EmailDeliveryStatus;
import com.rit.performance.service.EmailEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EmailNotificationRepository extends JpaRepository<EmailNotification, Long> {
    boolean existsByDeduplicationKey(String deduplicationKey);

    @Query("""
            select email from EmailNotification email
            where email.status = com.rit.performance.service.EmailDeliveryStatus.PENDING
              and email.retryCount < :maxRetries
              and (email.nextAttemptDate is null or email.nextAttemptDate <= :now)
            order by email.createdDate asc
            """)
    List<EmailNotification> findReadyToSend(@Param("now") LocalDateTime now,
            @Param("maxRetries") int maxRetries, Pageable pageable);

    @Query("""
            select email from EmailNotification email
            where (:eventType is null or email.eventType = :eventType)
              and (:status is null or email.status = :status)
              and (:recipient is null or lower(email.recipientEmail) like lower(concat('%', :recipient, '%')))
              and (:cycleId is null or email.cycleId = :cycleId)
              and (:reviewId is null or email.employeeReviewId = :reviewId)
              and (:query is null or lower(email.subject) like lower(concat('%', :query, '%'))
                   or lower(email.body) like lower(concat('%', :query, '%'))
                   or lower(email.recipientEmail) like lower(concat('%', :query, '%')))
            """)
    Page<EmailNotification> search(@Param("eventType") EmailEventType eventType,
            @Param("status") EmailDeliveryStatus status, @Param("recipient") String recipient,
            @Param("cycleId") Long cycleId, @Param("reviewId") Long reviewId,
            @Param("query") String query, Pageable pageable);
}
