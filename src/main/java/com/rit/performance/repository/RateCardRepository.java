package com.rit.performance.repository;

import com.rit.performance.entity.RateCard;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RateCardRepository extends JpaRepository<RateCard, Long> {
    boolean existsByPositionTitleId(Long positionTitleId);
    @EntityGraph(attributePaths = {"client"})
    @Query("select rateCard from RateCard rateCard")
    List<RateCard> findAllWithDetails();
    @EntityGraph(attributePaths = {"client"})
    @Query("select rateCard from RateCard rateCard where rateCard.id = :id")
    Optional<RateCard> findByIdWithDetails(Long id);
}
