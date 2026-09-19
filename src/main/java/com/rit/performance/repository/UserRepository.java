package com.rit.performance.repository;

import com.rit.performance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findForSecurityUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where lower(u.username) = lower(:username)")
    Optional<User> findForAuthentication(@org.springframework.data.repository.query.Param("username") String username);

    @Query("select u.id from User u where lower(u.employee.email) = :email and upper(u.status) = 'ACTIVE'")
    Optional<Long> findActiveIdByEmail(@org.springframework.data.repository.query.Param("email") String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    Optional<User> findByEmployeeId(Long employeeId);

    List<User> findByRoleNameIgnoreCaseAndStatusIgnoreCaseOrderByIdAsc(String roleName, String status);
}
