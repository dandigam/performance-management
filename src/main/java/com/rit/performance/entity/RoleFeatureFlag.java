package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "role_feature_flags", uniqueConstraints =
        @UniqueConstraint(name = "uk_role_feature", columnNames = {"feature_id", "role_id"}))
@Getter
@Setter
public class RoleFeatureFlag {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feature_id", nullable = false)
    private Feature feature;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private LookupValue role;
    @Column(nullable = false)
    private boolean enabled;
}
