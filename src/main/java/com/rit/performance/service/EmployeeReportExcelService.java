package com.rit.performance.service;

import com.rit.performance.dto.report.EmployeeReportExportData;
import com.rit.performance.dto.report.ReportResultColumn;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class EmployeeReportExcelService {

    private static final int MIN_COLUMN_CHARACTERS = 12;
    private static final int MAX_COLUMN_CHARACTERS = 50;

    public void write(EmployeeReportExportData data, OutputStream outputStream) throws IOException {
        write(data, "Employee Workforce", outputStream);
    }

    public void write(EmployeeReportExportData data, String sheetName, OutputStream outputStream) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            workbook.setCompressTempFiles(true);
            writeSheet(workbook, data, sheetName);
            workbook.write(outputStream);
        }
    }

    private void writeSheet(Workbook workbook, EmployeeReportExportData data, String sheetName) {
        Sheet sheet = workbook.createSheet(sheetName);
        CellStyle headerStyle = headerStyle(workbook);
        CellStyle dateStyle = dateStyle(workbook);
        List<ReportResultColumn> columns = data.columns();
        int[] widths = new int[columns.size()];

        Row header = sheet.createRow(0);
        for (int index = 0; index < columns.size(); index++) {
            String label = columns.get(index).label();
            Cell cell = header.createCell(index);
            cell.setCellValue(label);
            cell.setCellStyle(headerStyle);
            widths[index] = label.length();
        }

        int rowIndex = 1;
        for (Map<String, Object> values : data.content()) {
            Row row = sheet.createRow(rowIndex++);
            for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                ReportResultColumn column = columns.get(columnIndex);
                Object value = values.get(column.key());
                Cell cell = row.createCell(columnIndex);
                if (value instanceof LocalDate date) {
                    cell.setCellValue(Date.valueOf(date));
                    cell.setCellStyle(dateStyle);
                    widths[columnIndex] = Math.max(widths[columnIndex], 11);
                } else if (value instanceof Number number) {
                    cell.setCellValue(number.doubleValue());
                    widths[columnIndex] = Math.max(widths[columnIndex], String.valueOf(value).length());
                } else if (value != null) {
                    String text = String.valueOf(value);
                    cell.setCellValue(text);
                    widths[columnIndex] = Math.max(widths[columnIndex], text.length());
                }
            }
        }

        sheet.createFreezePane(0, 1);
        if (!columns.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, rowIndex - 1), 0, columns.size() - 1));
        }
        for (int index = 0; index < widths.length; index++) {
            int characters = Math.max(MIN_COLUMN_CHARACTERS, Math.min(MAX_COLUMN_CHARACTERS, widths[index] + 2));
            sheet.setColumnWidth(index, characters * 256);
        }
    }

    private static CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private static CellStyle dateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("mmm d, yyyy"));
        return style;
    }
}
