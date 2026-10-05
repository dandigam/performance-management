package com.rit.performance.repository;

import com.rit.performance.entity.CsxEmployee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CsxEmployeeRepository extends JpaRepository<CsxEmployee, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"client", "designation", "businessUnit"})
    @org.springframework.data.jpa.repository.Query("""
            select e from CsxEmployee e where (:clientId is null or e.client.id = :clientId)
            order by e.firstName, e.lastName, e.id
            """)
    java.util.List<CsxEmployee> findForClient(
            @org.springframework.data.repository.query.Param("clientId") Long clientId);

    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
