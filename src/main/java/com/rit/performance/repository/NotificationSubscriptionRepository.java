package com.rit.performance.repository;

import com.rit.performance.entity.NotificationSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationSubscriptionRepository extends JpaRepository<NotificationSubscription, Long> {
    @org.springframework.data.jpa.repository.Query("""
            select s from NotificationSubscription s
            join fetch s.category c join fetch c.lookupType t
            where s.active = true and c.active = true and t.active = true
            and upper(t.code) = 'NOTIFICATION_CATEGORY' and upper(c.code) in :codes
            order by s.id
            """)
    List<NotificationSubscription> findActiveForCodes(
            @org.springframework.data.repository.query.Param("codes") java.util.Collection<String> codes);
    List<NotificationSubscription> findAllByOrderByIdAsc();
    boolean existsByCategoryId(Long categoryId);
    boolean existsByCategoryIdAndIdNot(Long categoryId, Long id);
}
