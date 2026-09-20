package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@org.hibernate.annotations.DynamicUpdate
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "session_version", nullable = false, columnDefinition = "bigint default 0")
    private long sessionVersion;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private LookupValue role;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", unique = true)
    private Employee employee;
}
