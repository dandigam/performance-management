package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee_addresses", uniqueConstraints =
        @UniqueConstraint(name = "uk_employee_address_employee", columnNames = "employee_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EmployeeAddress extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_employee_addresses_employee"))
    private Employee employee;

    @Column(name = "address_line_1", length = 200)
    private String addressLine1;
    @Column(name = "address_line_2", length = 200)
    private String addressLine2;
    @Column(length = 100)
    private String city;
    @Column(length = 100)
    private String state;
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    @Column(length = 100)
    private String country;
}
