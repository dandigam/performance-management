package com.rit.performance.repository;

import com.rit.performance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    Optional<User> findByEmployeeId(Long employeeId);

    List<User> findByRoleNameIgnoreCaseAndStatusIgnoreCaseOrderByIdAsc(String roleName, String status);
}
