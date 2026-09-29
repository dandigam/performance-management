package com.rit.performance.controller;

import com.rit.performance.dto.report.EmployeeWorkforceReportResponse;
import com.rit.performance.dto.report.EmployeeReportExportData;
import com.rit.performance.dto.report.GenericReportResponse;
import com.rit.performance.dto.report.ReportDefinitionResponse;
import com.rit.performance.dto.report.ReportExportRequest;
import com.rit.performance.dto.report.ReportQueryRequest;
import com.rit.performance.service.EmployeeReportExcelService;
import com.rit.performance.service.SowMilestoneReportService;
import com.rit.performance.service.EmployeeReportQueryService;
import com.rit.performance.service.EmployeeWorkforceReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final EmployeeWorkforceReportService employeeWorkforceReportService;
    private final EmployeeReportQueryService employeeReportQueryService;
    private final EmployeeReportExcelService employeeReportExcelService;
    private final SowMilestoneReportService sowMilestoneReportService;

    @GetMapping("/sows/definition")
    public ResponseEntity<ReportDefinitionResponse> getSowReportDefinition() {
        return ResponseEntity.ok(sowMilestoneReportService.definition());
    }

    @PostMapping("/sows/query")
    public ResponseEntity<GenericReportResponse> querySowReport(
            @Valid @RequestBody ReportQueryRequest request) {
        return ResponseEntity.ok(sowMilestoneReportService.query(request));
    }

    @PostMapping("/sows/export")
    public ResponseEntity<StreamingResponseBody> exportSowReport(
            @Valid @RequestBody ReportExportRequest request) {
        String filename = "sow-milestones-" + LocalDate.now() + ".xlsx";
        EmployeeReportExportData data = sowMilestoneReportService.exportData(request);
        StreamingResponseBody body = outputStream ->
                employeeReportExcelService.write(data, "SOW and Milestones", outputStream);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
    }

    @GetMapping("/employees/definition")
    public ResponseEntity<ReportDefinitionResponse> getEmployeeReportDefinition() {
        return ResponseEntity.ok(employeeReportQueryService.definition());
    }

    @PostMapping("/employees/query")
    public ResponseEntity<GenericReportResponse> queryEmployeeReport(
            @Valid @RequestBody ReportQueryRequest request) {
        return ResponseEntity.ok(employeeReportQueryService.query(request));
    }

    @PostMapping("/employees/export")
    public ResponseEntity<StreamingResponseBody> exportEmployeeReport(
            @Valid @RequestBody ReportExportRequest request) {
        String filename = "employee-workforce-" + LocalDate.now() + ".xlsx";
        EmployeeReportExportData data = employeeReportQueryService.exportData(request);
        StreamingResponseBody body = outputStream -> employeeReportExcelService.write(data, outputStream);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
    }

    @GetMapping("/employees")
    public ResponseEntity<EmployeeWorkforceReportResponse> getEmployeeWorkforceReport(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long designationId,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) String workMode,
            @RequestParam(required = false) String workLocation,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "employeeName,asc") String sort
    ) {
        return ResponseEntity.ok(employeeWorkforceReportService.getReport(
                search, departmentId, designationId, employmentType, workMode,
                workLocation, status, page, size, sort));
    }
}
