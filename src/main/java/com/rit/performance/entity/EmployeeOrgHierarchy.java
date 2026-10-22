package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "employee_org_hierarchy", uniqueConstraints =
        @UniqueConstraint(name = "uk_org_hierarchy_employee", columnNames = "employee_id"))
@Getter
@Setter
@NoArgsConstructor
public class EmployeeOrgHierarchy extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_org_hierarchy_employee"))
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporting_manager_id",
            foreignKey = @ForeignKey(name = "fk_org_hierarchy_manager"))
    private Employee reportingManager;
}
