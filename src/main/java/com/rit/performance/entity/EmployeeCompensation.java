package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_compensations", indexes = {
        @Index(name = "idx_employee_compensation_employee", columnList = "employee_id"),
        @Index(name = "idx_employee_compensation_current", columnList = "employee_id,is_current")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EmployeeCompensation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_employee_compensations_employee"))
    private Employee employee;

    @Column(name = "pay_type")
    private String payType;

    @Column(name = "hourly_rate")
    private BigDecimal hourlyRate;

    @Column(name = "annual_salary")
    private BigDecimal annualSalary;

    @Column
    private String currency;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column
    private String reason;

    @Column(name = "is_current")
    private boolean current;
}
