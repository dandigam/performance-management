package com.rit.performance.config;

import com.rit.performance.entity.LookupType;
import com.rit.performance.entity.LookupValue;
import com.rit.performance.repository.LookupTypeRepository;
import com.rit.performance.repository.LookupValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CoreLookupDataInitializer implements ApplicationRunner {

    private final LookupTypeRepository lookupTypeRepository;
    private final LookupValueRepository lookupValueRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        LookupType workMode = ensureType(
                "WORK_MODE", "Work Mode", "Employee work mode options");
        ensureValue(workMode, "OFFSHORE", "Offshore", "Employee works offshore", 1);
        ensureValue(workMode, "ONSITE", "Onsite", "Employee works onsite", 2);

        LookupType workLocation = ensureType(
                "WORK_LOCATION", "Work Location", "Employee work location options");
        ensureValue(workLocation, "REMOTE", "Remote", "Employee works remotely", 1);
        ensureValue(workLocation, "OFFICE", "Office", "Employee works from the office", 2);
        ensureValue(workLocation, "HYBRID", "Hybrid",
                "Employee works from the office and remotely", 3);

        LookupType employeeStatus = ensureType(
                "EMPLOYEE_STATUS", "Employee Status", "Employee status options");
        ensureValue(employeeStatus, "ACTIVE", "Active", "Employee is active", 1);
        ensureValue(employeeStatus, "INACTIVE", "Inactive", "Employee is inactive", 2);

        LookupType employmentType = ensureType(
                "EMPLOYMENT_TYPE", "Employment Type", "Employee employment type options");
        ensureValue(employmentType, "FULL_TIME", "Full Time", "Full-time employee", 1);
        ensureValue(employmentType, "CONTRACT", "Contract", "Contract employee", 2);
    }

    private LookupType ensureType(String code, String name, String description) {
        LookupType type = lookupTypeRepository.findByCodeIgnoreCase(code)
                .orElseGet(LookupType::new);
        type.setCode(code);
        type.setName(name);
        type.setDescription(description);
        type.setActive(true);
        return lookupTypeRepository.save(type);
    }

    private void ensureValue(
            LookupType type,
            String code,
            String name,
            String description,
            int displayOrder
    ) {
        LookupValue value = lookupValueRepository
                .findByLookupTypeIdAndCodeIgnoreCase(type.getId(), code)
                .orElseGet(LookupValue::new);
        value.setLookupType(type);
        value.setCode(code);
        value.setName(name);
        value.setDescription(description);
        value.setDisplayOrder(displayOrder);
        value.setActive(true);
        lookupValueRepository.save(value);
    }
}
