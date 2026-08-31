package com.rit.performance.service;

import com.rit.performance.dto.AiChatRequest;
import com.rit.performance.dto.AiChatResponse;
import com.rit.performance.dto.EmployeeBasicInfoResponse;
import com.rit.performance.dto.CycleDetailsResponse;
import com.rit.performance.dto.ReviewProgressEmployeeResponse;
import com.rit.performance.dto.ReviewProgressResponse;
import com.rit.performance.dto.VendorResponse;
import com.rit.performance.dto.DocumentResponse;
import com.rit.performance.dto.response.SowResponse;
import com.rit.performance.exception.InvalidOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
public class AiChatServiceImpl implements AiChatService {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final EmployeeService employeeService;
    private final CycleDetailsService cycleDetailsService;
    private final EmployeeReviewService employeeReviewService;
    private final VendorService vendorService;
    private final SowService sowService;

    public AiChatServiceImpl(
            EmployeeService employeeService,
            CycleDetailsService cycleDetailsService,
            EmployeeReviewService employeeReviewService,
            VendorService vendorService,
            SowService sowService,
            @Value("${app.ai.gemini.api-key:}") String apiKey,
            @Value("${app.ai.gemini.model:gemini-flash-latest}") String model,
            @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
        this.employeeService = employeeService;
        this.cycleDetailsService = cycleDetailsService;
        this.employeeReviewService = employeeReviewService;
        this.vendorService = vendorService;
        this.sowService = sowService;
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        if (requestsRestrictedFinancialData(request.message())) {
            return new AiChatResponse(
                    "I can’t provide bank account, routing, payment, or other financial details.", model);
        }
        if (requestsSensitiveInformation(request.message())) {
            return new AiChatResponse(
                    "I can’t provide any sensitive information, including financial, personal, or authentication details.",
                    model);
        }
        if (requestsInappropriateContent(request.message())) {
            return new AiChatResponse(
                    "I can’t help with sexual, abusive, hateful, violent, or otherwise inappropriate content.", model);
        }
        if (apiKey.isBlank()) {
            throw new InvalidOperationException(
                    "AI is not configured. Set GEMINI_API_KEY before using this endpoint.");
        }

        try {
            GeminiGenerateResponse response = restClient.post()
                    .uri("/models/" + model + ":generateContent")
                    .header("X-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new GeminiGenerateRequest(
                            new GeminiContent(List.of(new GeminiPart(systemPrompt()))),
                            List.of(new GeminiContent(List.of(new GeminiPart(request.message().trim()))))))
                    .retrieve()
                    .body(GeminiGenerateResponse.class);

            if (response == null || response.candidates() == null || response.candidates().isEmpty()
                    || response.candidates().get(0).content() == null
                    || response.candidates().get(0).content().parts() == null
                    || response.candidates().get(0).content().parts().isEmpty()
                    || response.candidates().get(0).content().parts().get(0).text() == null) {
                throw new InvalidOperationException("AI returned an empty response. Please try again.");
            }
            return new AiChatResponse(response.candidates().get(0).content().parts().get(0).text().trim(), model);
        } catch (HttpClientErrorException.TooManyRequests ex) {
            throw new InvalidOperationException(
                    "AI provider rate limit reached. Please wait a minute and check the Gemini API billing and usage limits.");
        } catch (RestClientException ex) {
            throw new InvalidOperationException("AI request failed. Please try again later.");
        }
    }

    /**
     * Reuses existing read-only application services. It deliberately excludes emails,
     * phone numbers, vendor/bank details, and other sensitive employee data.
     */
    private String systemPrompt() {
        return """
                You are RIT AI, the internal performance-management portal assistant.
                For portal questions, use only the live portal summary supplied below.
                Always answer normal, non-sensitive general-information questions using your general knowledge.
                Do not say general information is unavailable merely because it is absent from the portal summary.
                Never invent employee, project, review, salary, banking, or personal portal data.
                If a portal-data request is not covered by the summary, say that it is unavailable.
                Keep answers concise and never expose private contact or financial information.
                Today's server date is: %s

                LIVE PORTAL SUMMARY
                Employees:
                %s

                Vendors:
                %s

                Statements of work:
                %s

                Review progress:
                %s
                """.formatted(LocalDate.now(), employeeSummary(), vendorSummary(), sowSummary(),
                reviewSummary());
    }

    private String employeeSummary() {
        List<EmployeeBasicInfoResponse> employees = employeeService.getBasicInfo();
        if (employees.isEmpty()) return "No employees found.";
        return employees.stream()
                .map(employee -> "- " + value(employee.getEmployeeName())
                        + " | status: " + value(employee.getStatus())
                        + " | role: " + value(employee.getRoleName())
                        + " | department: " + value(employee.getDepartmentName())
                        + " | SOW: " + value(employee.getSowName()))
                .collect(Collectors.joining("\n"));
    }

    private String reviewSummary() {
        List<CycleDetailsResponse> activeCycles = cycleDetailsService.getAllCycleDetails().stream()
                .filter(cycle -> "ACTIVE".equalsIgnoreCase(cycle.getStatus())
                        || "PUBLISHED".equalsIgnoreCase(cycle.getStatus()))
                .toList();
        if (activeCycles.isEmpty()) return "No active review cycles found.";

        return activeCycles.stream()
                .map(this::reviewSummaryForCycle)
                .collect(Collectors.joining("\n"));
    }

    private String vendorSummary() {
        List<VendorResponse> vendors = vendorService.getAll();
        if (vendors.isEmpty()) return "No vendors found.";
        return vendors.stream()
                .map(vendor -> "- " + value(vendor.getCompanyName())
                        + " | status: " + value(vendor.getStatus())
                        + " | currency: " + value(vendor.getCurrency()))
                .collect(Collectors.joining("\n"));
    }

    private String sowSummary() {
        List<SowResponse> sows = sowService.getAll();
        if (sows.isEmpty()) return "No statements of work found.";
        return sows.stream()
                .map(sow -> "- " + value(sow.getSowName())
                        + " (" + value(sow.getSowCode()) + ")"
                        + " | status: " + value(sow.getStatus())
                        + " | CSX project: " + value(sow.getCsxProjectId())
                        + " | start: " + value(sow.getStartDate())
                        + " | end: " + value(sow.getEndDate())
                        + " | documents: " + sowDocuments(sow))
                .collect(Collectors.joining("\n"));
    }

    private String sowDocuments(SowResponse sow) {
        if (sow.getDocumentList() == null || sow.getDocumentList().isEmpty()) return "none";
        return sow.getDocumentList().stream()
                .map(document -> documentSummary(document))
                .collect(Collectors.joining(", "));
    }

    private String documentSummary(DocumentResponse document) {
        return value(document.getDocumentName()) + " [" + value(document.getDocumentType())
                + ", " + value(document.getFileType()) + "]";
    }

    private String reviewSummaryForCycle(CycleDetailsResponse cycle) {
        ReviewProgressResponse progress = employeeReviewService.getReviewProgress(cycle.getId());
        List<ReviewProgressEmployeeResponse> overdue = progress.getEmployees().stream()
                .filter(employee -> employee.getAssessments() != null
                        && employee.getAssessments().stream().anyMatch(assessment -> assessment.isOverdue()))
                .toList();
        String overdueEmployees = overdue.isEmpty() ? "none"
                : overdue.stream().map(employee -> value(employee.getEmployeeName())
                        + " (pending with " + value(employee.getPendingWithRole()) + ")")
                        .collect(Collectors.joining(", "));
        return "- " + value(progress.getCycleName()) + " | total reviews: " + progress.getEmployees().size()
                + " | overdue: " + overdue.size() + " | overdue employees: " + overdueEmployees;
    }

    private static String value(Object value) {
        return value == null || value.toString().isBlank() ? "Not available" : value.toString();
    }

    private static boolean requestsSensitiveInformation(String message) {
        String normalized = message.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("salary")
                || normalized.contains("compensation")
                || normalized.contains("social security")
                || normalized.contains("ssn")
                || normalized.contains("date of birth")
                || normalized.contains("home address")
                || normalized.contains("phone number")
                || normalized.contains("email address")
                || normalized.contains("password")
                || normalized.contains("access token")
                || normalized.contains("jwt token");
    }

    private static boolean requestsRestrictedFinancialData(String message) {
        String normalized = message.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("bank detail")
                || normalized.contains("bank account")
                || normalized.contains("account number")
                || normalized.contains("routing number")
                || normalized.contains("iban")
                || normalized.contains("swift code")
                || normalized.contains("ifsc")
                || normalized.contains("payment detail")
                || normalized.contains("payment information");
    }

    private static boolean requestsInappropriateContent(String message) {
        String normalized = message.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("sexual")
                || normalized.contains("porn")
                || normalized.contains("nude")
                || normalized.contains("explicit content")
                || normalized.contains("harass")
                || normalized.contains("hate speech")
                || normalized.contains("racial slur")
                || normalized.contains("threaten")
                || normalized.contains("kill ")
                || normalized.contains("self harm")
                || normalized.contains("suicide");
    }


    private record GeminiGenerateRequest(GeminiContent systemInstruction, List<GeminiContent> contents) {
    }

    private record GeminiContent(List<GeminiPart> parts) {
    }

    private record GeminiPart(String text) {
    }

    private record GeminiGenerateResponse(List<GeminiCandidate> candidates) {
    }

    private record GeminiCandidate(GeminiContent content) {
    }

}
