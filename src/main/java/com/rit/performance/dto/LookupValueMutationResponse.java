package com.rit.performance.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LookupValueMutationResponse {
    private Long id;
    private Long lookupTypeId;
    private String code;
    private String name;
    private String requirementType;
    private String status;
    private boolean active;
}
