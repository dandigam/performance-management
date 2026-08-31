package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "lookup_values",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lookup_value_type_code",
                columnNames = {"lookup_type_id", "code"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LookupValue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lookup_type_id", nullable = false)
    private LookupType lookupType;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 1;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
