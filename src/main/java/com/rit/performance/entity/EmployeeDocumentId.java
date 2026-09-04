package com.rit.performance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EmployeeDocumentId implements Serializable {
    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "document_id")
    private Long documentId;
}
