package com.rit.performance.service;

import com.rit.performance.entity.Employee;
import com.rit.performance.entity.EmployeeOrgHierarchy;
import com.rit.performance.repository.EmployeeOrgHierarchyRepository;
import com.rit.performance.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EmployeeOrgHierarchyExportService {
    private final EmployeeRepository employees;
    private final EmployeeOrgHierarchyRepository hierarchy;

    @Transactional(readOnly = true)
    public byte[] export() throws IOException {
        List<Employee> allEmployees = employees.findAll();
        Map<Long, Employee> byId = new HashMap<>();
        allEmployees.forEach(employee -> byId.put(employee.getId(), employee));
        Map<Long, Long> managerIds = new HashMap<>();
        for (EmployeeOrgHierarchy row : hierarchy.findAll()) {
            if (row.getReportingManager() != null) {
                managerIds.put(row.getEmployee().getId(), row.getReportingManager().getId());
            }
        }
        allEmployees.sort(Comparator.comparing(this::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(employee -> text(employee.getRitId()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Employee::getId));

        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Organization Hierarchy");
            var font = workbook.createFont();
            font.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(font);
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
            String[] headers = {"Employee ID", "Employee name", "Reporting manager 1", "Reporting manager 2", "Status"};
            int[] widths = {18, 30, 30, 30, 14};
            Row header = sheet.createRow(0);
            for (int column = 0; column < headers.length; column++) {
                var cell = header.createCell(column, CellType.STRING);
                cell.setCellValue(headers[column]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(column, widths[column] * 256);
            }
            int index = 1;
            for (Employee employee : allEmployees) {
                Long first = managerIds.get(employee.getId());
                if (Objects.equals(first, employee.getId()) || !byId.containsKey(first)) first = null;
                Long second = first == null ? null : managerIds.get(first);
                if (Objects.equals(second, employee.getId()) || Objects.equals(second, first)) second = null;
                String[] values = {text(employee.getRitId()), name(employee),
                        name(byId.get(first)), name(byId.get(second)), status(employee.getStatus())};
                Row row = sheet.createRow(index++);
                for (int column = 0; column < values.length; column++) {
                    // STRING cells preserve RIT IDs and never interpret employee values as formulas.
                    var cell = row.createCell(column, CellType.STRING);
                    cell.setCellStyle(textStyle);
                    cell.setCellValue(values[column]);
                }
            }
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, index - 1, 0, headers.length - 1));
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private String name(Employee employee) {
        return employee == null ? "" : (text(employee.getFirstName()).trim() + " "
                + text(employee.getLastName()).trim()).trim();
    }

    private String text(String value) { return value == null ? "" : value; }

    private String status(String value) {
        if ("ACTIVE".equalsIgnoreCase(value)) return "Active";
        if ("INACTIVE".equalsIgnoreCase(value)) return "Inactive";
        return text(value);
    }
}
