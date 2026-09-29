package com.rit.performance.repository;

import com.rit.performance.entity.EmployeeOnboarding;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class OnboardingPersistenceTest extends EmployeeSummaryQueryTest {
    @Test void hrQueueIncludesBothStatusesAndFiltersBeforePaging() {
        for (String status : java.util.List.of("INVITED", "IN_PROGRESS", "SUBMITTED")) {
            var item = new EmployeeOnboarding();
            item.setEmployee(employee(status, "PENDING")); item.setStatus(status);
            item.setRequestActor("hr"); item.setIdempotencyKey(java.util.UUID.randomUUID().toString());
            item.setRequestHash("c".repeat(64)); item.setRequiredSections("[]");
            em.persist(item);
        }
        em.flush(); em.clear();
        var onboardings = new org.springframework.data.jpa.repository.support.JpaRepositoryFactory(em)
                .getRepository(EmployeeOnboardingRepository.class);
        var page = org.springframework.data.domain.PageRequest.of(0, 1,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"));
        assertThat(onboardings.findQueue(null, page).getTotalElements()).isEqualTo(3);
        assertThat(onboardings.findQueue("INVITED", page).getContent()).singleElement()
                .satisfies(item -> assertThat(item.getStatus()).isEqualTo("INVITED"));
        assertThat(onboardings.findQueue("SUBMITTED", page).getTotalElements()).isEqualTo(1);
    }

    private com.rit.performance.entity.Employee employee(String name, String status) {
        var employee = new com.rit.performance.entity.Employee();
        employee.setFirstName(name);
        employee.setEmail(name.toLowerCase() + "@example.com");
        employee.setStatus(status);
        employee.setJoiningDate(today.minusYears(1));
        em.persist(employee);
        return employee;
    }

    @Test void getIncludesPreviouslyPersistedEmploymentWithoutOnboardingPut() throws Exception {
        var employee = employee("ExistingEmployee", "PENDING");
        var other = employee("OtherEmployee", "PENDING");
        var onboarding = new EmployeeOnboarding();
        onboarding.setEmployee(employee);
        onboarding.setRequestActor("hr");
        onboarding.setIdempotencyKey(java.util.UUID.randomUUID().toString());
        onboarding.setRequestHash("b".repeat(64));
        onboarding.setRequiredSections("[\"employment-history\"]");
        em.persist(onboarding);
        em.persist(com.rit.performance.entity.EmployeeExperience.builder().employee(employee)
                .companyName("Napier").position("Sr Java").location("Hyderabad")
                .fromDate(java.time.LocalDate.of(2016, 1, 1))
                .endDate(java.time.LocalDate.of(2017, 1, 1)).build());
        em.persist(com.rit.performance.entity.EmployeeExperience.builder().employee(other)
                .companyName("Other company").build());
        em.flush();
        em.clear();
        var current = org.mockito.Mockito.mock(com.rit.performance.service.CurrentEmployeeService.class);
        org.mockito.Mockito.when(current.currentEmployee()).thenReturn(employee);
        var repositories = new org.springframework.data.jpa.repository.support.JpaRepositoryFactory(em);
        var service = new com.rit.performance.service.OnboardingSelfService(current,
                repositories.getRepository(EmployeeOnboardingRepository.class),
                repositories.getRepository(LookupValueRepository.class),
                repositories.getRepository(EmployeeAddressRepository.class),
                repositories.getRepository(EmployeeEducationRepository.class),
                repositories.getRepository(EmployeeExperienceRepository.class), repositories.getRepository(BankAccountRepository.class), org.mockito.Mockito.mock(com.rit.performance.service.EmailNotificationService.class), repositories.getRepository(UserRepository.class));
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                new com.rit.performance.controller.OnboardingSelfController(service)).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/onboarding/me"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.details.employmentRecords.length()").value(1))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.details.employmentRecords[0].companyName").value("Napier"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.details.employmentRecords[0].fromDate").value("2016-01-01"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.details.employmentRecords[0].endDate").value("2017-01-01"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.completedSections[0]").value("employment-history"));
    }

    @Test void reservationAndCompletionCommitVersionOne() {
        EmployeeOnboarding onboarding = new EmployeeOnboarding();
        onboarding.setRequestActor("hr");
        onboarding.setIdempotencyKey(java.util.UUID.randomUUID().toString());
        onboarding.setRequestHash("a".repeat(64));
        onboarding.setRequiredSections("[]");
        em.persist(onboarding);
        em.flush();
        assertThat(onboarding.getVersion()).isZero();
        onboarding.setOriginalResponse("{\"version\":1}");
        onboarding.setReviewComments("Correct employment dates.");
        onboarding.setReviewedBy("hr@example.test");
        onboarding.setReviewedAt(java.time.Instant.now());
        em.flush();
        em.clear();
        assertThat(em.find(EmployeeOnboarding.class, onboarding.getId()).getVersion()).isEqualTo(1);
        assertThat(em.find(EmployeeOnboarding.class, onboarding.getId()).getReviewComments()).isEqualTo("Correct employment dates.");
        assertThat(em.find(EmployeeOnboarding.class, onboarding.getId()).getReviewedBy()).isEqualTo("hr@example.test");
    }
}
