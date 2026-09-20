package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_request_approvals")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
public class LeaveRequestApproval {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_request_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_leave_approval_request"))
    private LeaveRequest leaveRequest;
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_level", nullable = false, length = 10)
    private LeaveApprovalLevel approvalLevel;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approver_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_leave_approval_employee"))
    private Employee approver;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LeaveApprovalAction action;
    @Column(length = 2000)
    private String comments;
    @Column(name = "action_at", nullable = false)
    private LocalDateTime actionAt;
    @CreatedDate @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @CreatedBy @Column(name = "created_by", updatable = false)
    private Long createdBy;
}
