package com.rit.performance.service;

import com.rit.performance.dto.report.EmployeeReportExportData;
import com.rit.performance.dto.report.ReportResultColumn;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeReportExcelServiceTest {

    @Test
    void exportsRequestedColumnsInOrderWithFormattingAndBlankNulls() throws Exception {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("employeeName", "Charan Kovvuru");
        row.put("workMode", "Onsite");
        row.put("departmentName", null);
        row.put("joiningDate", LocalDate.of(2023, 7, 12));
        EmployeeReportExportData data = new EmployeeReportExportData(
                List.of(
                        new ReportResultColumn("employeeName", "Employee", "TEXT"),
                        new ReportResultColumn("workMode", "Work mode", "LOOKUP"),
                        new ReportResultColumn("departmentName", "Department", "TEXT"),
                        new ReportResultColumn("joiningDate", "Joining date", "DATE")),
                List.of(row));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new EmployeeReportExcelService().write(data, output);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()))) {
            var sheet = workbook.getSheet("Employee Workforce");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Employee");
            assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Work mode");
            assertThat(sheet.getRow(0).getCell(2).getStringCellValue()).isEqualTo("Department");
            assertThat(sheet.getRow(0).getCell(3).getStringCellValue()).isEqualTo("Joining date");
            assertThat(sheet.getRow(0).getCell(0).getCellStyle().getFontIndex())
                    .isNotEqualTo(0);
            assertThat(workbook.getFontAt(sheet.getRow(0).getCell(0).getCellStyle().getFontIndex()).getBold())
                    .isTrue();
            assertThat(sheet.getPaneInformation().isFreezePane()).isTrue();
            assertThat(sheet.getCTWorksheet().isSetAutoFilter()).isTrue();
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Charan Kovvuru");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Onsite");
            assertThat(sheet.getRow(1).getCell(2).getCellType()).isEqualTo(CellType.BLANK);
            assertThat(sheet.getRow(1).getCell(3).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(sheet.getRow(1).getCell(3).getLocalDateTimeCellValue().toLocalDate())
                    .isEqualTo(LocalDate.of(2023, 7, 12));
        }
    }
}
