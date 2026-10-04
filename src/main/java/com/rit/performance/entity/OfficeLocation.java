package com.rit.performance.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="office_locations") @Getter @Setter
public class OfficeLocation extends BaseEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="office_name",nullable=false,length=150) private String officeName;
 @Column(name="address_line1",nullable=false,length=255) private String addressLine1;
 @Column(name="address_line2",length=255) private String addressLine2;
 @Column(nullable=false,length=100) private String city;
 @Column(name="state_region",length=100) private String stateRegion;
 @Column(name="postal_code",length=20) private String postalCode;
 @Column(name="country_code",nullable=false,length=2) private String countryCode;
 @Column(name="time_zone",nullable=false,length=100) private String timeZone;
 @Column(length=30) private String phone;
 @Column(length=254) private String email;
 @Column(nullable=false) private boolean active=true;
}
