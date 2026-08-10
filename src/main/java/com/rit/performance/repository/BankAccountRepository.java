package com.rit.performance.repository;

import com.rit.performance.entity.BankAccount;
import com.rit.performance.entity.BankAccountOwnerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findByOwnerTypeAndOwnerIdOrderByIsPrimaryDescIdAsc(
            BankAccountOwnerType ownerType, Long ownerId);

    Optional<BankAccount> findFirstByOwnerTypeAndOwnerIdAndIsPrimaryTrueAndActiveTrue(
            BankAccountOwnerType ownerType, Long ownerId);
}
