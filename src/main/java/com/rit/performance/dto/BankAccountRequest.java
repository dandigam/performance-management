package com.rit.performance.dto;

import com.rit.performance.entity.BankAccountOwnerType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccountRequest {

    @NotNull(message = "ownerType is required")
    private BankAccountOwnerType ownerType;

    @NotNull(message = "ownerId is required")
    @Positive(message = "ownerId must be positive")
    private Long ownerId;

    @Size(max = 100)
    private String bankCountry;

    @Size(max = 10)
    private String currency;

    @Size(max = 50)
    private String paymentMethod;

    @Size(max = 200)
    private String accountHolderName;

    @Size(max = 200)
    private String bankName;

    @Size(max = 50)
    private String accountType;

    @Size(max = 65535)
    private String accountNumberEncrypted;

    @Size(min = 4, max = 4, message = "accountNumberLast4 must contain exactly 4 characters")
    private String accountNumberLast4;

    @Size(max = 65535)
    private String routingNumberEncrypted;

    @Size(max = 20)
    private String ifscCode;

    @Size(max = 200)
    private String branchName;

    @Email(message = "remittanceEmail must be valid")
    @Size(max = 150)
    private String remittanceEmail;

    private Boolean isPrimary;
    private Boolean active;
}
