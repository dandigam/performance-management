package com.rit.performance.repository;

import com.rit.performance.entity.EmployeeReviewAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeReviewAnswerRepository extends JpaRepository<EmployeeReviewAnswer, Long> {
}
