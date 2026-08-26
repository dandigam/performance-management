package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lookup_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LookupType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @PrePersist
    void prePersist() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }
}
