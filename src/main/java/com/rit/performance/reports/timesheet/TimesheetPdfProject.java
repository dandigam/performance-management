package com.rit.performance.reports.timesheet;

import java.math.BigDecimal;
import java.util.List;

public record TimesheetPdfProject(Long projectId, String projectName, String sowName,
                                  String milestoneName, String designationName, String clientName,
                                  List<TimesheetPdfDay> days, BigDecimal regularHours,
                                  BigDecimal overtimeHours, BigDecimal totalHours) {}
