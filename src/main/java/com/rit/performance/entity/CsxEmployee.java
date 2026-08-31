package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "csx_employees",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_csx_employee_email", columnNames = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CsxEmployee extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(length = 100)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "designation_id",
            foreignKey = @ForeignKey(name = "fk_csx_employee_designation")
    )
    private LookupValue designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "business_unit_id",
            foreignKey = @ForeignKey(name = "fk_csx_employee_business_unit")
    )
    private LookupValue businessUnit;

    @Column(length = 20)
    private String status;
    @PrePersist
    void prePersist() {
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }
}
