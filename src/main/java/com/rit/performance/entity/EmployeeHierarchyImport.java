package com.rit.performance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "employee_hierarchy_imports")
@Getter
@Setter
public class EmployeeHierarchyImport extends BaseEntity {
    @Id
    private String id;
    @Column(nullable = false)
    private String uploader;
    private String fileName;
    private Instant uploadedAt;
    private String status;
    private int insertedEmployees;
    private int insertedHierarchy;
    private int updatedHierarchy;
    private int unchanged;
    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] workbook;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String outcomes;
}
