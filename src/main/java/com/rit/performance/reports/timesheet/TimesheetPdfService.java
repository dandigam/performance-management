package com.rit.performance.reports.timesheet;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TimesheetPdfService {
    private final TimesheetPdfDataService dataService;
    private final SpringTemplateEngine templateEngine;

    public TimesheetPdfResult generate(Long timesheetId, Authentication authentication) {
        TimesheetPdfDocument document = dataService.loadAuthorized(timesheetId, authentication);
        Context context = new Context(Locale.US);
        context.setVariable("timesheet", document);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            String html = templateEngine.process("pdf/timesheet", context);
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return new TimesheetPdfResult(buildFilename(document), output.toByteArray());
        } catch (Exception exception) {
            throw new TimesheetPdfException("The timesheet PDF could not be generated.", exception);
        }
    }

    private String buildFilename(TimesheetPdfDocument document) {
        String employee = document.employeeNumber() == null || document.employeeNumber().isBlank()
                ? "employee" : document.employeeNumber().replaceAll("[^A-Za-z0-9_-]", "-");
        return "timesheet-" + employee + "-" + document.periodStart() + ".pdf";
    }
}
