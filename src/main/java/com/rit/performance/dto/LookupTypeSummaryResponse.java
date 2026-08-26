package com.rit.performance.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LookupTypeSummaryResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private boolean active;
    private LocalDateTime createdDate;
}
