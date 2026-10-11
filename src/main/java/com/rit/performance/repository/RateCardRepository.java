package com.rit.performance.repository;

import com.rit.performance.entity.RateCard;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RateCardRepository extends JpaRepository<RateCard, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from RateCard r
            where r.client.id = :clientId and r.effectiveFrom = :startDate
              and r.positionTitleId = :positionTitleId
              and r.mainSkill.id = :mainSkillId
              and (r.locationId = :locationId or (r.locationId is null and :locationId is null))
              and (r.seniorityId = :seniorityId or (r.seniorityId is null and :seniorityId is null))
            order by r.id
            """)
    List<RateCard> findExactDuplicates(Long clientId, java.time.LocalDate startDate,
                                      Long positionTitleId, Long mainSkillId,
                                      Long locationId, Long seniorityId);
    boolean existsByPositionTitleId(Long positionTitleId);
    @EntityGraph(attributePaths = {"client", "mainSkill"})
    @Query("select rateCard from RateCard rateCard")
    List<RateCard> findAllWithDetails();
    @EntityGraph(attributePaths = {"client", "mainSkill"})
    @Query("select rateCard from RateCard rateCard where rateCard.id = :id")
    Optional<RateCard> findByIdWithDetails(Long id);
}
