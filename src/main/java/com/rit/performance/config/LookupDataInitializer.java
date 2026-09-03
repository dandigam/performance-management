package com.rit.performance.config;

import com.rit.performance.entity.LookupType;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.repository.LookupTypeRepository;
import com.rit.performance.repository.LookupValueRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(0)
public class LookupDataInitializer implements ApplicationRunner {

    private final LookupTypeRepository typeRepository;
    private final LookupValueRepository valueRepository;

    public LookupDataInitializer(LookupTypeRepository typeRepository, LookupValueRepository valueRepository) {
        this.typeRepository = typeRepository;
        this.valueRepository = valueRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        LookupType reviewType = type("REVIEW_TYPE", "Review Types");
        LookupType applicableType = type("APPLICABLE_TYPE", "Applicable Types");
        type("RATING_SCALE", "Rating Scale");
        type("PIP_STATUS", "PIP Status");
        LookupType reviewStatus = type("REVIEW_STATUS", "Review Status");
        LookupType seniority = type("SENIORITY", "Seniority");
        LookupType location = type("LOCATION", "Location");
        LookupType sowType = type("SOW_TYPE", "SOW Type");
        LookupType sowStatus = type("SOW_STATUS", "SOW Status");
        LookupType designation = type("DESIGNATION", "Designation");

        value(reviewType, "ANNUAL", "Annual Review", 1);
        value(reviewType, "MID_YEAR", "Mid-Year Review", 2);
        value(reviewType, "QUARTERLY", "Quarterly Review", 3);
        value(reviewType, "PROBATION", "Probation Review", 4);
        value(reviewType, "CUSTOM", "Custom", 5);

        value(applicableType, "ALL", "All Employees", 1);
        value(applicableType, "DEPARTMENT", "Department", 2);
        value(applicableType, "DESIGNATION", "Designation", 3);
        value(applicableType, "EMPLOYEE", "Specific Employees", 4);

        value(reviewStatus, "DRAFT", "Draft", 1);
        value(reviewStatus, "ACTIVE", "Active", 2);
        value(reviewStatus, "COMPLETED", "Completed", 3);
        value(reviewStatus, "ARCHIVED", "Archived", 4);

        value(seniority, "ASSOCIATE", "Associate", 1);
        value(seniority, "JUNIOR", "Junior", 2);
        value(seniority, "MID_LEVEL", "Mid-Level", 3);
        value(seniority, "SENIOR", "Senior", 4);
        value(seniority, "LEAD", "Lead", 5);
        value(seniority, "STAFF", "Staff", 6);
        value(seniority, "PRINCIPAL", "Principal", 7);
        value(seniority, "ARCHITECT", "Architect", 8);
        value(seniority, "MANAGER", "Manager", 9);
        value(seniority, "DIRECTOR", "Director", 10);

        value(location, "ONSITE", "Onsite", 1);
        value(location, "OFFSHORE", "Offshore", 2);

        value(sowType, "SUPPORT", "Support", 1);
        value(sowType, "DEVELOPMENT", "Development", 2);
        value(sowType, "DISCOVERY", "Discovery", 3);

        value(sowStatus, "DRAFT", "Draft", 1);
        value(sowStatus, "WAITING_FOR_APPROVAL", "Waiting for Approval", 2);
        value(sowStatus, "ACTIVE", "Active", 3);
        value(sowStatus, "ON_HOLD", "On Hold", 4);
        value(sowStatus, "COMPLETED", "Completed", 5);
        value(sowStatus, "CANCELLED", "Cancelled", 6);

        value(designation, "JAVA_DEVELOPER", "Java Developer", 1);
        value(designation, "REACT_DEVELOPER", "React Developer", 2);
        value(designation, "FULL_STACK_DEVELOPER", "Full Stack Developer", 3);
        value(designation, "QA_AUTOMATION_ENGINEER", "QA Automation Engineer", 4);
        value(designation, "DEVOPS_ENGINEER", "DevOps Engineer", 5);
        value(designation, "L2_SUPPORT_ENGINEER", "L2 Support Engineer", 6);

    }

    private LookupType type(String code, String name) {
        return typeRepository.findByCodeIgnoreCase(code)
                .orElseGet(() -> typeRepository.save(LookupType.builder()
                        .code(code)
                        .name(name)
                        .build()));
    }

    private void value(LookupType type, String code, String name, int displayOrder) {
        if (!valueRepository.existsByLookupTypeIdAndCodeIgnoreCase(type.getId(), code)) {
            valueRepository.save(LookupValue.builder()
                    .lookupType(type)
                    .code(code)
                    .name(name)
                    .displayOrder(displayOrder)
                    .build());
        }
    }
}
