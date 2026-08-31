package com.rit.performance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Common audit metadata for persistent domain entities.
 *
 * <p>The actor identifiers are populated by application services. Timestamps are
 * managed here so they cannot be supplied or accidentally reset by callers.</p>
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public abstract class BaseEntity {

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @CreatedDate
    // Nullable during rollout so Hibernate can add this column to populated tables.
    // Spring Data auditing supplies the value for every newly persisted entity.
    @Column(name = "created_on", updatable = false)
    private LocalDateTime createdOn;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    @LastModifiedDate
    // Existing rows are backfilled separately; new writes are always audited.
    @Column(name = "updated_on")
    private LocalDateTime updatedOn;
}
