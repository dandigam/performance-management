package com.rit.performance.service;

import com.rit.performance.entity.Employee;
import com.rit.performance.entity.Sow;
import com.rit.performance.entity.Timesheet;
import com.rit.performance.entity.TimesheetEmployeeProject;
import com.rit.performance.entity.User;
import com.rit.performance.entity.EmployeeLeavePolicy;
import com.rit.performance.entity.LeaveRequest;
import com.rit.performance.entity.SowMilestonePositionAssignment;
import com.rit.performance.entity.SowInvoice;
import com.rit.performance.entity.SowInvoicePayment;
import com.rit.performance.entity.PerformanceCycles;
import com.rit.performance.entity.EmployeeReview;
import com.rit.performance.entity.EmployeeReviewAssessment;
import com.rit.performance.entity.FinalRating;
import com.rit.performance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class ApplicationEmailFactory {
    private static final DateTimeFormatter EMAIL_DATE = DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.US);
    private final SpringTemplateEngine htmlTemplateEngine;
    private final UserRepository users;

    @Value("${app.mail.footer:Regards, RailInfo Tech}")
    private String footer;

    @Value("${app.mail.base-url:http://localhost:5173}")
    private String frontendUrl;



    public ApplicationEmailFactory(UserRepository users) {
        this.users = users;
        ClassLoaderTemplateResolver htmlResolver = new ClassLoaderTemplateResolver();
        htmlResolver.setPrefix("templates/email/");
        htmlResolver.setSuffix(".html");
        htmlResolver.setTemplateMode("HTML");
        htmlResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        htmlResolver.setCacheable(true);
        htmlTemplateEngine = new SpringTemplateEngine();
        htmlTemplateEngine.setTemplateResolver(htmlResolver);
    }

    public ApplicationEmail employeeAdminNotification(Employee employee, boolean created) {
        String action = created ? "created" : "updated";
        Context context = context();
        String status = Objects.toString(employee.getStatus(), "");
        context.setVariable("notificationTitle", "Employee notification");
        context.setVariable("recipientName", "Admin team");
        context.setVariable("actionMessage", "Employee " + employeeName(employee) + " has been " + action + ".");
        context.setVariable("details", List.of(
                new EmailDetail("Employee name", employeeName(employee)),
                new EmailDetail("Employee ID", Objects.toString(employee.getRitId(), "")),
                new EmailDetail("Email", Objects.toString(employee.getEmail(), ""))));
        context.setVariable("status", status);
        context.setVariable("statusBackground", statusBackground(status));
        context.setVariable("statusColor", statusColor(status));
        context.setVariable("actionUrl", url("/login"));
        return new ApplicationEmail(null, "Employee " + action + ": " + employeeName(employee),
                htmlTemplateEngine.process("resource-operation", context), true,
                NotificationRecipientResolver.GLOBAL_CATEGORY);
    }

    public ApplicationEmail employeeCreated(Employee employee, User user) {
        Context context = context();
        context.setVariable("employeeName", employeeName(employee));
        context.setVariable("employeeId", employee.getRitId());
        context.setVariable("username", user.getUsername());
        context.setVariable("loginUrl", url("/login"));
        return new ApplicationEmail(employee.getEmail(), "Your RIT employee account has been created",
                htmlTemplateEngine.process("employee-created", context), true);
    }

    public ApplicationEmail employeeUpdated(Employee employee) {
        Context context = context();
        context.setVariable("recipientName", employeeName(employee));
        context.setVariable("message", "Your employee profile has been updated. Please sign in to review your details.");
        context.setVariable("actionUrl", url("/login"));
        return new ApplicationEmail(employee.getEmail(), "Your RIT employee profile has been updated",
                htmlTemplateEngine.process("manual-notification", context), true);
    }

    public ApplicationEmail passwordReset(String recipient, String resetLink) {
        Context context = context();
        context.setVariable("resetLink", resetLink);
        return new ApplicationEmail(recipient, "Reset your RIT Performance Management password",
                htmlTemplateEngine.process("password-reset", context), true);
    }

    public ApplicationEmail passwordOtp(String recipient, String otp, boolean setup) {
        Context context = context();
        context.setVariable("otp", otp);
        context.setVariable("purpose", setup ? "set up your account password" : "reset your password");
        return new ApplicationEmail(recipient, "Your RailInfo Tech verification code",
                htmlTemplateEngine.process("password-otp", context), true);
    }

    public ApplicationEmail userInvitation(String recipient, String username, String invitationUrl) {
        Context context = context();
        context.setVariable("username", username);
        context.setVariable("invitationUrl", invitationUrl);
        return new ApplicationEmail(recipient, "Your RailInfo Tech account invitation",
                htmlTemplateEngine.process("user-invitation", context), true);
    }

    public ApplicationEmail onboardingInvitation(Employee employee, String designationName, String invitationUrl) {
        Context context = employeeContext(employee);
        context.setVariable("designationName", designationName);
        context.setVariable("joiningDate", formatDate(employee.getJoiningDate()));
        context.setVariable("username", employee.getEmail());
        context.setVariable("invitationUrl", invitationUrl);
        return new ApplicationEmail(employee.getEmail(), "Welcome to RailInfo Tech! Complete your onboarding",
                htmlTemplateEngine.process("onboarding-invitation", context), true);
    }

    public ApplicationEmail onboardingSubmitted(com.rit.performance.entity.EmployeeOnboarding onboarding,
            String recipient, boolean resubmission) {
        var employee = onboarding.getEmployee();
        String action = resubmission ? "resubmitted" : "submitted";
        return manualNotification(recipient, "HR/Admin team",
                "Onboarding " + action + " for review — " + employee.getRitId(),
                employeeName(employee) + " (" + employee.getRitId() + ") has " + action
                        + " onboarding for review.\nSubmitted at: " + onboarding.getSubmittedAt()
                        + "\n\nPlease sign in and open the employee's onboarding review.", url("/login")).withCategory("ONBOARDING");
    }

    public ApplicationEmail onboardingChangesRequested(Employee employee, String comments) {
        return manualNotification(employee.getEmail(), employeeName(employee),
                "Changes requested for your onboarding",
                "HR has requested changes to your onboarding:\n\n" + comments
                        + "\n\nPlease sign in, update your saved details and resubmit for review.", url("/login")).withCategory("ONBOARDING");
    }

    public ApplicationEmail manualNotification(String recipient, String recipientName,
            String subject, String message, String actionUrl) {
        Context context = context();
        context.setVariable("recipientName", recipientName);
        context.setVariable("message", message);
        context.setVariable("actionUrl", actionUrl);
        return new ApplicationEmail(recipient, subject,
                htmlTemplateEngine.process("manual-notification", context), true, "ALL_NOTIFICATIONS");
    }

    public ApplicationEmail passwordChanged(User user) {
        Employee employee = user.getEmployee();
        Context context = employeeContext(employee);
        context.setVariable("loginUrl", url("/login"));
        return new ApplicationEmail(employee.getEmail(), "Your password has been changed",
                htmlTemplateEngine.process("password-changed", context), true);
    }

    public ApplicationEmail cyclePublished(PerformanceCycles cycle, Employee employee) {
        Context context = employeeContext(employee);
        context.setVariable("cycleName", cycle.getCycleName());
        context.setVariable("actionUrl", url("/login"));
        return new ApplicationEmail(employee.getEmail(), cycle.getCycleName() + " is now open",
                htmlTemplateEngine.process("performance-cycle-published", context), true, "PERFORMANCE_REVIEW");
    }

    public ApplicationEmail assessmentReady(EmployeeReview review, EmployeeReviewAssessment assessment) {
        Employee reviewer = assessment.getAssessorEmployee();
        Context context = employeeContext(reviewer);
        context.setVariable("employeeName", employeeName(review.getEmployee()));
        context.setVariable("roleName", assessment.getAssessorRole() == null
                ? "reviewer" : assessment.getAssessorRole().getName());
        context.setVariable("dueDate", assessment.getDueDate() == null ? null : formatDate(assessment.getDueDate()));
        context.setVariable("reopenReason", assessment.getReopenReason());
        context.setVariable("actionUrl", url("/login"));
        return new ApplicationEmail(reviewer.getEmail(), employeeName(review.getEmployee()) + "'s review is ready",
                htmlTemplateEngine.process("assessment-ready", context), true, "PERFORMANCE_REVIEW");
    }

    public ApplicationEmail assessmentReopened(EmployeeReview review, EmployeeReviewAssessment assessment,
            Object newDueDate, String reason) {
        Employee reviewer = assessment.getAssessorEmployee();
        Context context = employeeContext(reviewer);
        context.setVariable("employeeName", employeeName(review.getEmployee()));
        context.setVariable("cycleName", review.getPerformanceCycle().getCycleName());
        context.setVariable("roleName", assessment.getAssessorRole() == null
                ? "review" : assessment.getAssessorRole().getName() + " assessment");
        String dueDate = newDueDate instanceof LocalDate date ? formatDate(date) : Objects.toString(newDueDate, "Not available");
        context.setVariable("dueDate", dueDate);
        context.setVariable("reason", reason);
        context.setVariable("actionUrl", url("/login"));
        return new ApplicationEmail(reviewer.getEmail(), "Assessment reopened until " + dueDate,
                htmlTemplateEngine.process("assessment-reopened", context), true, "PERFORMANCE_REVIEW");
    }

    public ApplicationEmail resultPublished(FinalRating rating) {
        EmployeeReview review = rating.getEmployeeReview();
        Employee employee = review.getEmployee();
        Context context = employeeContext(employee);
        context.setVariable("cycleName", review.getPerformanceCycle().getCycleName());
        context.setVariable("actionUrl", url("/login"));
        return new ApplicationEmail(employee.getEmail(), "Your performance review result is available",
                htmlTemplateEngine.process("performance-result-published", context), true, "PERFORMANCE_REVIEW");
    }

    public ApplicationEmail sowCreated(Sow sow) {
        return sowNotification(sow, "created", "A new SOW has been created");
    }

    public ApplicationEmail sowUpdated(Sow sow, String changeType) {
        return sowNotification(sow, changeType,
                "status updated".equals(changeType) ? "SOW status changed" : "A SOW has been updated");
    }

    private ApplicationEmail sowNotification(Sow sow, String changeType, String subject) {
        Context context = context();
        context.setVariable("recipientName", "team");
        boolean created = "created".equals(changeType);
        Long actor = created ? sow.getCreatedBy() : sow.getUpdatedBy();
        var timestamp = created ? sow.getCreatedOn() : sow.getUpdatedOn();
        context.setVariable("subject", subject + ": " + sow.getSowName() + " (" + sow.getId() + ")");
        context.setVariable("heading", subject);
        String actorName = actorName(actor);
        String status = sow.getStatus() == null ? "Not available" : sow.getStatus().getName();
        String actionMessage = switch (changeType) {
            case "status updated" -> "The status of " + sow.getSowName() + " was changed to " + status + " by " + actorName + ".";
            case "signature updated" -> "The signature for " + sow.getSowName() + " was updated by " + actorName + ".";
            default -> sow.getSowName() + " was " + changeType + " by " + actorName + ".";
        };
        context.setVariable("actionMessage", actionMessage);
        context.setVariable("sowId", sow.getId());
        context.setVariable("timestampLabel", created ? "Created on:" : "Updated on:");
        context.setVariable("formattedTimestamp", timestamp == null ? "Not available" : timestamp.format(
                java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", Locale.ENGLISH)));
        context.setVariable("sowName", sow.getSowName());
        context.setVariable("clientName", sow.getClient() == null ? "" : sow.getClient().getClientName());
        context.setVariable("status", status);
        context.setVariable("startDate", formatDate(sow.getStartDate()));
        context.setVariable("endDate", formatDate(sow.getEndDate()));
        context.setVariable("sowUrl", url("/sows/" + sow.getId()));
        return new ApplicationEmail(null, subject + ": " + sow.getSowName()
                + " (" + sow.getId() + ")",
                htmlTemplateEngine.process("sow-notification", context), true, "SOW");
    }

    public List<ApplicationEmail> timesheetWorkflow(Timesheet timesheet, String action, String comments) {
        List<ApplicationEmail> emails = new ArrayList<>();
        Set<String> recipients = new HashSet<>();
        addTimesheetEmail(emails, recipients, timesheet, timesheet.getEmployee(),
                "Employee", action, comments);

        TimesheetEmployeeProject setup = timesheet.getTimesheetEmployeeProject();
        if (setup != null) {
            addTimesheetEmail(emails, recipients, timesheet, setup.getLevel1Approver(),
                    "Level 1 approver", action, comments);
            addTimesheetEmail(emails, recipients, timesheet, setup.getLevel2Approver(),
                    "Level 2 approver", action, comments);
        }
        return emails;
    }

    private void addTimesheetEmail(List<ApplicationEmail> emails, Set<String> recipients,
            Timesheet timesheet, Employee recipient, String recipientRole, String action, String comments) {
        if (recipient == null || recipient.getEmail() == null || recipient.getEmail().isBlank()) {
            return;
        }
        String email = recipient.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!recipients.add(email)) {
            return;
        }

        Context context = context();
        context.setVariable("recipientName", employeeName(recipient));
        context.setVariable("recipientRole", recipientRole);
        context.setVariable("action", action);
        context.setVariable("employeeName", employeeName(timesheet.getEmployee()));
        context.setVariable("employeeId", timesheet.getEmployee().getRitId());
        context.setVariable("periodStart", formatDate(timesheet.getWeekStartDate()));
        context.setVariable("periodEnd", formatDate(timesheet.getWeekEndDate()));
        context.setVariable("totalHours", timesheet.getTotalHours());
        context.setVariable("status", displayStatus(timesheet.getStatus().name()));
        context.setVariable("comments", comments);
        context.setVariable("timesheetUrl", url("/timesheets"));
        emails.add(new ApplicationEmail(email, "Timesheet " + action,
                htmlTemplateEngine.process("timesheet-workflow", context), true, "TIMESHEET"));
    }

    public List<ApplicationEmail> milestoneAssignment(Employee employee,
            SowMilestonePositionAssignment assignment, String action) {
        List<EmailDetail> details = List.of(
                new EmailDetail("SOW", assignment.getMilestonePosition().getSow().getSowName()),
                new EmailDetail("Milestone", assignment.getMilestonePosition().getMilestone().getMilestoneName()),
                new EmailDetail("Position", assignment.getMilestonePosition().getPositionName()),
                new EmailDetail("Assignment period", dateRange(
                        assignment.getAssignmentStartDate(), assignment.getAssignmentEndDate())));
        String employeeMessage = switch (action.toLowerCase(Locale.ROOT)) {
            case "assigned" -> "You have been assigned to a milestone.";
            case "unassigned" -> "You have been unassigned from a milestone.";
            default -> "Your milestone resource assignment was " + action + ".";
        };
        return resourceOperation(employee, "Resource assignment " + action,
                employeeMessage, "The employee's milestone resource assignment was " + action + ".",
                details, Objects.toString(assignment.getStatus(), ""), "/sows", "RESOURCE_ALLOCATION");
    }

    public List<ApplicationEmail> timesheetSetup(TimesheetEmployeeProject setup, String action) {
        List<EmailDetail> details = List.of(
                new EmailDetail("Work type", Objects.toString(setup.getWorkType(), "")),
                new EmailDetail("SOW", setup.getSow() == null ? "" : setup.getSow().getSowName()),
                new EmailDetail("Milestone", setup.getMilestone() == null ? "" : setup.getMilestone().getMilestoneName()),
                new EmailDetail("Setup period", dateRange(setup.getStartDate(), setup.getEndDate())));
        return resourceOperation(setup.getEmployee(), "Timesheet setup " + action,
                "Your timesheet setup was " + action + ".",
                "The employee's timesheet setup was " + action + ".", details,
                Objects.toString(setup.getStatus(), ""), "/timesheets", "TIMESHEET");
    }

    public List<ApplicationEmail> leavePolicy(EmployeeLeavePolicy assignment, String action) {
        List<EmailDetail> details = List.of(
                new EmailDetail("Policy", assignment.getLeavePolicy().getPolicyName()),
                new EmailDetail("Effective period", dateRange(
                        assignment.getEffectiveFrom(), assignment.getEffectiveTo())));
        return resourceOperation(assignment.getEmployee(), "Leave setup " + action,
                "Your leave setup was " + action + ".",
                "The employee's leave setup was " + action + ".", details,
                Objects.toString(assignment.getStatus(), ""), "/leave", "LEAVE");
    }

    public List<ApplicationEmail> leaveRequest(LeaveRequest request, String action, String comments) {
        List<EmailDetail> details = new ArrayList<>();
        details.add(new EmailDetail("Leave type", request.getLeaveType().getName()));
        details.add(new EmailDetail("Leave period", dateRange(request.getFromDate(), request.getToDate())));
        details.add(new EmailDetail("Total hours", Objects.toString(request.getTotalHours(), "")));
        if (comments != null && !comments.isBlank()) {
            details.add(new EmailDetail("Comments", comments));
        }
        return resourceOperation(request.getEmployee(), "Leave request " + action,
                "Your leave request was " + action + ".",
                "The employee's leave request was " + action + ".", details,
                Objects.toString(request.getStatus(), ""), "/leave", "LEAVE");
    }

    private List<ApplicationEmail> resourceOperation(Employee employee, String subject,
            String employeeMessage, String adminMessage, List<EmailDetail> details,
            String status, String path, String category) {
        List<ApplicationEmail> emails = new ArrayList<>();
        Set<String> recipients = new HashSet<>();
        addResourceOperationEmail(emails, recipients, employee.getEmail(), employeeName(employee),
                subject, employeeMessage, details, status, path);
        if (emails.isEmpty()) {
            addResourceOperationEmail(emails, recipients, null, "Team",
                    subject, adminMessage, details, status, path);
        }
        return emails.stream().map(email -> email.withCategory(category)).toList();
    }

    private void addResourceOperationEmail(List<ApplicationEmail> emails, Set<String> recipients,
            String recipient, String recipientName, String subject, String actionMessage,
            List<EmailDetail> details, String status, String path) {
        String email = recipient == null || recipient.isBlank() ? null
                : recipient.trim().toLowerCase(Locale.ROOT);
        if (!recipients.add(email)) {
            return;
        }
        Context context = context();
        context.setVariable("recipientName", recipientName);
        context.setVariable("actionMessage", actionMessage);
        context.setVariable("details", details);
        context.setVariable("status", status);
        context.setVariable("statusBackground", statusBackground(status));
        context.setVariable("statusColor", statusColor(status));
        context.setVariable("actionUrl", url(path));
        emails.add(new ApplicationEmail(email, subject,
                htmlTemplateEngine.process("resource-operation", context), true));
    }

    public ApplicationEmail invoice(SowInvoice invoice, String action) {
        Context context = context();
        context.setVariable("action", action);
        context.setVariable("invoiceId", invoice.getId());
        context.setVariable("invoiceNumber", Objects.toString(invoice.getCsxInvoiceNumber(), ""));
        context.setVariable("sowName", invoice.getSow().getSowName());
        context.setVariable("milestoneName", invoice.getMilestone().getMilestoneName());
        context.setVariable("dateLabel", "Invoice date");
        context.setVariable("amountLabel", "Invoice amount");
        context.setVariable("statusLabel", "Status");
        context.setVariable("invoiceDate", formatDate(invoice.getMilestoneInvoiceDate()));
        context.setVariable("invoiceAmount", invoice.getMilestoneInvoiceAmount());
        context.setVariable("raisedAmount", invoice.getInvoiceRaisedAmount());
        context.setVariable("status", invoice.getInvoiceStatus());
        context.setVariable("actionUrl", url("/sows"));
        return new ApplicationEmail(null, "Invoice " + action,
                htmlTemplateEngine.process("invoice-notification", context), true, "INVOICE");
    }

    public ApplicationEmail invoicePayment(SowInvoicePayment payment, String action) {
        SowInvoice invoice = payment.getInvoice();
        Context context = context();
        context.setVariable("action", "payment " + action);
        context.setVariable("invoiceId", invoice.getId());
        context.setVariable("invoiceNumber", Objects.toString(invoice.getCsxInvoiceNumber(), ""));
        context.setVariable("sowName", invoice.getSow().getSowName());
        context.setVariable("milestoneName", invoice.getMilestone().getMilestoneName());
        context.setVariable("dateLabel", "Payment date");
        context.setVariable("amountLabel", "Received amount");
        context.setVariable("statusLabel", "Payment reference");
        context.setVariable("invoiceDate", formatDate(payment.getPaymentDate()));
        context.setVariable("invoiceAmount", payment.getReceivedAmount());
        context.setVariable("raisedAmount", "");
        context.setVariable("status", Objects.toString(payment.getPaymentReference(), ""));
        context.setVariable("actionUrl", url("/sows"));
        return new ApplicationEmail(null, "Invoice payment " + action,
                htmlTemplateEngine.process("invoice-notification", context), true, "INVOICE");
    }

    private Context context() {
        Context context = new Context(Locale.US);
        context.setVariable("footer", footer);
        return context;
    }

    private Context employeeContext(Employee employee) {
        Context context = context();
        context.setVariable("recipientName", employeeName(employee));
        return context;
    }

    private String employeeName(Employee employee) {
        return (Objects.toString(employee.getFirstName(), "") + " "
                + (employee.getLastName() == null ? "" : employee.getLastName())).trim();
    }

    private String actorName(Long id) {
        if (id == null) return "System";
        return users.findCurrentUser(id).map(user -> {
            String name = user.getEmployee() == null ? "" : employeeName(user.getEmployee());
            return !name.isBlank() ? name : Objects.toString(user.getUsername(), "Unknown user");
        }).orElse("Unknown user");
    }

    private String displayStatus(String value) {
        if ("LEVEL1_APPROVED".equals(value)) return "Level 1 approved";
        String label = value.replace('_', ' ').toLowerCase(Locale.ROOT);
        return label.isEmpty() ? label : Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }

    private String url(String path) {
        return frontendUrl.replaceAll("/$", "") + path;
    }

    private String dateRange(LocalDate start, LocalDate end) {
        String startValue = formatDate(start);
        String endValue = formatDate(end);
        return endValue.isBlank() ? startValue : startValue + " – " + endValue;
    }

    private String formatDate(LocalDate value) {
        return value == null ? "" : EMAIL_DATE.format(value);
    }

    private String statusBackground(String status) {
        String value = status == null ? "" : status.toUpperCase(Locale.ROOT);
        if (value.contains("REJECT") || value.contains("CANCEL") || value.contains("INACTIVE")) return "#fee2e2";
        if (value.contains("PENDING") || value.contains("SUBMIT")) return "#fef3c7";
        return "#dcfce7";
    }

    private String statusColor(String status) {
        String value = status == null ? "" : status.toUpperCase(Locale.ROOT);
        if (value.contains("REJECT") || value.contains("CANCEL") || value.contains("INACTIVE")) return "#991b1b";
        if (value.contains("PENDING") || value.contains("SUBMIT")) return "#92400e";
        return "#166534";
    }

    public record EmailDetail(String label, String value) {
    }
}
