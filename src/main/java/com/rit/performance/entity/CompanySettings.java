package com.rit.performance.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="company_settings") @Getter @Setter
public class CompanySettings extends BaseEntity {
 @Id private Long id;
 @Column(name="portal_name",nullable=false,length=150) private String portalName;
 @ManyToOne(fetch=FetchType.LAZY)
 @JoinColumn(name="main_office_id",foreignKey=@ForeignKey(name="fk_company_main_office"))
 private OfficeLocation mainOffice;
}
