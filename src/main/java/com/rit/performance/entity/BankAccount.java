package com.rit.performance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "bank_account", indexes = {
        @Index(name = "idx_bank_account_owner", columnList = "owner_type,owner_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BankAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 20)
    private BankAccountOwnerType ownerType;

    // Polymorphic owner identifier only; intentionally has no database foreign key.
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "bank_country", length = 100)
    private String bankCountry;

    @Column(length = 10)
    private String currency;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "account_holder_name", length = 200)
    private String accountHolderName;

    @Column(name = "bank_name", length = 200)
    private String bankName;

    @Column(name = "account_type", length = 50)
    private String accountType;

    @Column(name = "account_number_encrypted", columnDefinition = "TEXT")
    private String accountNumberEncrypted;

    @Column(name = "account_number_last4", length = 4)
    private String accountNumberLast4;

    @Column(name = "routing_number_encrypted", columnDefinition = "TEXT")
    private String routingNumberEncrypted;

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;

    @Column(name = "branch_name", length = 200)
    private String branchName;

    @Column(name = "remittance_email", length = 150)
    private String remittanceEmail;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean isPrimary = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
