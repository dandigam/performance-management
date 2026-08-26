package com.rit.performance.repository;

import com.rit.performance.entity.Projects;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Projects, Long> {

    Optional<Projects> findByProjectCode(String projectCode);

    boolean existsByProjectCode(String projectCode);
}