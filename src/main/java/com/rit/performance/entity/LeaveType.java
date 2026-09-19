package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "leave_types", uniqueConstraints = @UniqueConstraint(name = "uk_leave_type_code", columnNames = "code"))
@AttributeOverrides({
    @AttributeOverride(name = "createdOn", column = @Column(name = "created_at", updatable = false)),
    @AttributeOverride(name = "updatedOn", column = @Column(name = "updated_at"))
})
@Getter @Setter
public class LeaveType extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50)
    private String code;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(length = 1000)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LeaveUnit unit;
    @Column(nullable = false)
    private boolean paid;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LeaveTypeStatus status = LeaveTypeStatus.ACTIVE;
}
