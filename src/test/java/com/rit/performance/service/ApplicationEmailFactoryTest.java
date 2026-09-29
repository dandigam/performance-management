package com.rit.performance.service;

import com.rit.performance.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationEmailFactoryTest {
    @Test
    void rendersPersonalizedOnboardingInvitation() throws Exception {
        ApplicationEmailFactory factory = new ApplicationEmailFactory();
        Employee employee = employee(101L, "Venkatesh", "Dandigam", "venkat@example.com");
        employee.setJoiningDate(LocalDate.of(2026, 1, 1));
        ApplicationEmail email = factory.onboardingInvitation(employee, "Senior Software Engineer",
                "https://example.com/reset-password?token=preview-only");
        assertTrue(email.html());
        assertEquals("Welcome to RailInfo Tech! Complete your onboarding", email.subject());
        assertTrue(email.body().contains("Welcome to RailInfo Tech!"));
        assertTrue(email.body().contains("Venkatesh Dandigam"));
        assertTrue(email.body().contains("Senior Software Engineer"));
        assertTrue(email.body().contains("Jan 1, 2026"));
        assertTrue(email.body().contains("cid:rit-logo"));
        assertTrue(email.body().contains("Set up your password"));
        assertTrue(email.body().contains("token=preview-only"));
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("target/email-preview"));
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/email-preview/onboarding.html"),
                email.body().replace("cid:rit-logo", "../../src/main/resources/email/rit-logo.png"));
        employee.setFirstName("<script>alert(1)</script>");
        assertFalse(factory.onboardingInvitation(employee, "Engineer", "https://example.com/setup")
                .body().contains("<script>"));
    }

    @Test
    void rendersBrandedUserInvitation() {
        ApplicationEmailFactory factory = new ApplicationEmailFactory();
        ReflectionTestUtils.setField(factory, "footer", "Regards, RailInfo Tech");

        ApplicationEmail email = factory.userInvitation(
                "finance@rit.com", "finance@rit.com", "http://localhost:5173/reset-password?token=test");

        assertTrue(email.html());
        assertTrue(email.body().contains("RailInfo Tech"));
        assertTrue(email.body().contains("finance@rit.com"));
        assertTrue(email.body().contains("reset-password?token=test"));
    }

    @Test
    void rendersTimesheetWorkflowForEmployeeAndBothApprovers() {
        ApplicationEmailFactory factory = new ApplicationEmailFactory();
        ReflectionTestUtils.setField(factory, "footer", "Regards, RIT");
        ReflectionTestUtils.setField(factory, "frontendUrl", "http://localhost:5173");

        Employee employee = employee(2L, "Charan", "Kovvuru", "employee@example.com");
        employee.setRitId("RIT03");
        Employee level1 = employee(3L, "Level", "One", "level1@example.com");
        Employee level2 = employee(4L, "Level", "Two", "level2@example.com");
        TimesheetEmployeeProject setup = new TimesheetEmployeeProject();
        setup.setLevel1Approver(level1);
        setup.setLevel2Approver(level2);
        Timesheet timesheet = new Timesheet();
        timesheet.setEmployee(employee);
        timesheet.setTimesheetEmployeeProject(setup);
        timesheet.setWeekStartDate(LocalDate.of(2026, 9, 20));
        timesheet.setWeekEndDate(LocalDate.of(2026, 9, 26));
        timesheet.setTotalHours(BigDecimal.valueOf(40));
        timesheet.setStatus(TimesheetStatus.SUBMITTED);

        List<ApplicationEmail> emails = factory.timesheetWorkflow(timesheet, "submitted", null);

        assertEquals(3, emails.size());
        assertTrue(emails.get(0).html());
        assertTrue(emails.get(0).body().contains("Timesheet"));
        assertTrue(emails.get(0).body().contains("submitted"));
        assertTrue(emails.get(0).body().contains("Charan Kovvuru"));
        assertTrue(emails.get(0).body().contains("RIT03"));
        assertFalse(emails.get(0).body().contains("Comments:"));
    }

    @Test
    void rendersSowNotificationAsBrandedHtml() {
        ApplicationEmailFactory factory = new ApplicationEmailFactory();
        ReflectionTestUtils.setField(factory, "footer", "Regards, RailInfo Tech");
        ReflectionTestUtils.setField(factory, "frontendUrl", "http://localhost:5173");

        Client client = new Client();
        client.setClientName("CSX");
        LookupValue status = new LookupValue();
        status.setName("Active");
        Sow sow = new Sow();
        sow.setId(21L);
        sow.setSowName("Test SOW");
        sow.setClient(client);
        sow.setStatus(status);
        sow.setStartDate(LocalDate.of(2026, 1, 1));
        sow.setEndDate(LocalDate.of(2026, 12, 31));

        ApplicationEmail email = factory.sowUpdated(sow, "updated");

        assertTrue(email.html());
        assertNull(email.recipient());
        assertEquals("SOW", email.category());
        assertTrue(email.subject().contains("Test SOW (SOW-21)"));
        assertTrue(email.body().contains("RailInfo Tech"));
        assertTrue(email.body().contains("http://localhost:5173/sows/21"));
    }

    private Employee employee(Long id, String firstName, String lastName, String email) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(email);
        return employee;
    }
}
