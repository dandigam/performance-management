package com.rit.performance.dto;

import com.rit.performance.entity.BankAccountOwnerType;
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
public class BankAccountSensitiveDetailsResponse {
    private Long id;
    private BankAccountOwnerType ownerType;
    private Long ownerId;
    private String accountNumber;
    private String routingNumber;
}
