package com.rit.performance.repository;

import com.rit.performance.entity.Client;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Client c where c.id = :id")
    Optional<Client> findForRateCardUpdate(Long id);
    boolean existsByClientNameIgnoreCase(String clientName);
    boolean existsByClientNameIgnoreCaseAndIdNot(String clientName, Long id);

    @Override
    @EntityGraph(attributePaths = "documents")
    List<Client> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = "documents")
    Optional<Client> findById(Long id);
}
