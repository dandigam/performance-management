package com.rit.performance.config;

import com.rit.performance.entity.LookupType;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.repository.LookupTypeRepository;
import com.rit.performance.repository.LookupValueRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
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
