package com.rit.performance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rit.performance.dto.EmployeeHierarchyImportResponse;
import com.rit.performance.dto.EmployeeHierarchyImportResponse.Row;
import com.rit.performance.entity.*;
import com.rit.performance.exception.*;
import com.rit.performance.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.time.Instant;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeHierarchyImportService {
    private static final String[] HEADERS = {"RACF ID", "First name", "Last name", "Email", "Reporting manager RACF ID"};
    private final EmployeeRepository employees;
    private final EmployeeOrgHierarchyRepository hierarchy;
    private final EmployeeHierarchyImportRepository imports;
    private final UserRepository users;
    private final EmployeeRoleRepository employeeRoles;
    private final LookupValueRepository lookups;
    private final PasswordEncoder passwordEncoder;
    private final Clock businessClock;
    private final ObjectMapper mapper = new ObjectMapper();

    public byte[] template() throws IOException {
        return workbook(Collections.singletonList(HEADERS));
    }

    @Transactional
    public EmployeeHierarchyImportResponse validate(MultipartFile file, String uploader) throws IOException {
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024
                || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx"))
            throw new InvalidOperationException("Upload a nonempty .xlsx file of at most 5 MB");
        var job = new EmployeeHierarchyImport();
        job.setId(UUID.randomUUID().toString());
        job.setUploader(uploader);
        String fileName = file.getOriginalFilename().replace('\\', '/');
        job.setFileName(fileName.substring(fileName.lastIndexOf('/') + 1));
        job.setUploadedAt(Instant.now());
        job.setWorkbook(file.getBytes());
        var result = preview(job);
        store(job, result);
        imports.save(job);
        return result;
    }

    public EmployeeHierarchyImportResponse status(String id) throws IOException {
        var job = find(id);
        return mapper.readValue(job.getOutcomes(), EmployeeHierarchyImportResponse.class);
    }

    @Transactional
    public EmployeeHierarchyImportResponse commit(String id) throws IOException {
        var job = imports.findForCommit(id).orElseThrow(() -> new ResourceNotFoundException("Import not found: " + id));
        if ("COMMITTED".equals(job.getStatus())) return status(id);
        // Use the same lock as every other hierarchy writer before reading the proposed graph.
        hierarchy.lockHierarchyWrites(PageRequest.of(0, 1));
        var result = preview(job);
        if (result.errors() > 0) {
            store(job, result);
            return result;
        }
        Map<String, Employee> byRacf = employeeIndex();
        for (Row row : result.rows()) {
            if (byRacf.containsKey(row.racfId())) continue;
            var employee = new Employee();
            employee.setCsxRacfId(row.racfId());
            employee.setFirstName(row.firstName());
            employee.setLastName(row.lastName());
            employee.setEmail(row.email());
            employee.setStatus("ACTIVE");
            employees.saveAndFlush(employee);
            String ritId = String.format("RIT%02d", employee.getId());
            if (employees.existsByRitIdIgnoreCase(ritId))
                throw new InvalidOperationException("Generated RIT ID already exists: " + ritId);
            employee.setRitId(ritId);
            byRacf.put(row.racfId(), employee);
        }
        // All employees now exist, so manager references are independent of file order.
        for (Row row : result.rows()) {
            Employee employee = byRacf.get(row.racfId());
            ensureAccountAndRole(employee);
            var existing = hierarchy.findByEmployee_Id(employee.getId());
            var link = existing.orElseGet(EmployeeOrgHierarchy::new);
            link.setEmployee(employee);
            if (!row.managerRacfId().isEmpty()) link.setReportingManager(byRacf.get(row.managerRacfId()));
            if (!"UNCHANGED".equals(row.action())) hierarchy.save(link);
        }
        hierarchy.flush();
        result = new EmployeeHierarchyImportResponse(id, "COMMITTED",
                "Hierarchy imported. Missing portal accounts and employee roles were created. No invitations were sent.", result.newEmployees(),
                result.insertedHierarchy(), result.updatedHierarchy(), result.unchanged(), 0, result.rows());
        store(job, result);
        return result;
    }

    private LookupValue accountRole(Employee employee) {
        var current = employee == null ? Optional.<EmployeeRole>empty()
                : employeeRoles.findFirstByEmployeeIdAndIsCurrentTrueOrderByEffectiveFromDesc(employee.getId());
        if (current.isPresent()) {
            var role = lookups.findById(current.get().getRoleId())
                    .orElseThrow(() -> new InvalidOperationException("Employee role lookup is missing"));
            if (!role.isActive() || !role.getLookupType().isActive()
                    || !"SYSTEM_ROLE".equalsIgnoreCase(role.getLookupType().getCode()))
                throw new InvalidOperationException("Employee role must be an active SYSTEM_ROLE lookup");
            return role;
        }
        return lookups.findByLookupTypeCodeIgnoreCaseAndCodeIgnoreCaseAndLookupTypeActiveTrueAndActiveTrue(
                "SYSTEM_ROLE", "EMPLOYEE")
                .orElseThrow(() -> new InvalidOperationException("EMPLOYEE role lookup is not configured"));
    }

    private void ensureAccountAndRole(Employee employee) {
        var account = users.findByEmployeeId(employee.getId()).orElse(null);
        var current = employeeRoles.findFirstByEmployeeIdAndIsCurrentTrueOrderByEffectiveFromDesc(employee.getId());
        if (account != null && current.isPresent()) return;
        // Preserve assigned roles and existing account security settings.
        var role = account == null ? accountRole(employee) : account.getRole();
        if (account == null) {
            String username = employee.getEmail().trim().toLowerCase(Locale.ROOT);
            if (users.existsByUsernameIgnoreCase(username))
                throw new InvalidOperationException("Employee email is already used by another portal account");
            account = new User();
            account.setEmployee(employee);
            account.setUsername(username);
            account.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            account.setStatus("ACTIVE".equalsIgnoreCase(employee.getStatus()) ? "INVITED" : "INACTIVE");
            account.setRole(role);
            users.save(account);
        }
        if (current.isEmpty()) {
            var mapping = new EmployeeRole();
            mapping.setEmployeeId(employee.getId());
            mapping.setRoleId(role.getId());
            mapping.setEffectiveFrom(LocalDate.now(businessClock));
            mapping.setIsCurrent(true);
            employeeRoles.save(mapping);
        }
        // Invitation generation and email delivery remain explicit user-management actions.
    }

    public byte[] errors(String id) throws IOException {
        var rows = new ArrayList<String[]>();
        rows.add(new String[]{"Excel row", "RACF ID", "Errors"});
        for (Row row : status(id).rows()) if (!row.errors().isEmpty())
            rows.add(new String[]{String.valueOf(row.rowNumber()), row.racfId(), String.join("; ", row.errors())});
        return workbook(rows);
    }

    private EmployeeHierarchyImport find(String id) {
        return imports.findById(id).orElseThrow(() -> new ResourceNotFoundException("Import not found: " + id));
    }

    private void store(EmployeeHierarchyImport job, EmployeeHierarchyImportResponse result) throws IOException {
        job.setStatus(result.status());
        job.setInsertedEmployees(result.newEmployees());
        job.setInsertedHierarchy(result.insertedHierarchy());
        job.setUpdatedHierarchy(result.updatedHierarchy());
        job.setUnchanged(result.unchanged());
        job.setOutcomes(mapper.writeValueAsString(result));
    }

    private Map<String, Employee> employeeIndex() {
        Map<String, Employee> index = new HashMap<>();
        for (Employee employee : employees.findAll()) {
            String racf = key(employee.getCsxRacfId());
            if (!racf.isEmpty() && index.putIfAbsent(racf, employee) != null)
                throw new InvalidOperationException("Existing employees have duplicate RACF ID: " + racf + ". Correct the data before importing.");
        }
        return index;
    }

    private EmployeeHierarchyImportResponse preview(EmployeeHierarchyImport job) throws IOException {
        List<Row> rows = parse(job.getWorkbook());
        Map<String, Employee> byRacf = employeeIndex();
        Map<String, Long> occurrences = rows.stream().collect(Collectors.groupingBy(Row::racfId, Collectors.counting()));
        Map<String, Long> emailCounts = rows.stream().filter(r -> !byRacf.containsKey(r.racfId()))
                .filter(r -> !r.email().isEmpty()).collect(Collectors.groupingBy(Row::email, Collectors.counting()));
        Set<String> available = new HashSet<>(byRacf.keySet());
        rows.forEach(row -> available.add(row.racfId()));
        Map<Long, String> keysById = new HashMap<>();
        employees.findAll().forEach(e -> keysById.put(e.getId(), "ID:" + e.getId()));
        byRacf.forEach((key, e) -> keysById.put(e.getId(), key));
        Map<String, String> graph = new HashMap<>();
        Map<String, EmployeeOrgHierarchy> links = new HashMap<>();
        hierarchy.findAll().forEach(link -> {
            String key = keysById.get(link.getEmployee().getId());
            links.put(key, link);
            if (link.getReportingManager() != null) graph.put(key, keysById.get(link.getReportingManager().getId()));
        });
        rows.forEach(row -> {
            if (!row.managerRacfId().isEmpty()) graph.put(row.racfId(), row.managerRacfId());
        });
        List<Row> checked = new ArrayList<>();
        for (Row row : rows) {
            List<String> errors = new ArrayList<>();
            boolean isNew = !byRacf.containsKey(row.racfId());
            if (row.racfId().isEmpty()) errors.add("RACF ID is required");
            if (row.racfId().length() > 255) errors.add("RACF ID exceeds 255 characters");
            if (occurrences.get(row.racfId()) > 1) errors.add("Duplicate RACF ID in file");
            if (isNew) {
                if (row.firstName().isEmpty()) errors.add("First name is required for new employees");
                if (row.lastName().isEmpty()) errors.add("Last name is required for new employees");
                if (row.firstName().length() > 255 || row.lastName().length() > 255) errors.add("Names must be at most 255 characters");
                if (row.email().length() > 254 || !row.email().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) errors.add("Valid email is required for new employees");
                if (!row.email().isEmpty() && (employees.existsByEmailIgnoreCase(row.email())
                        || users.existsByUsernameIgnoreCase(row.email()) || emailCounts.getOrDefault(row.email(), 0L) > 1))
                    errors.add("Email is already used by another employee, account, or new row");
            }
            Employee existingEmployee = byRacf.get(row.racfId());
            boolean needsAccount = isNew || users.findByEmployeeId(existingEmployee.getId()).isEmpty();
            if (needsAccount) {
                String email = isNew ? row.email() : Objects.toString(existingEmployee.getEmail(), "").trim().toLowerCase(Locale.ROOT);
                if (email.length() > 50 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
                    errors.add("Portal account requires a valid employee email of at most 50 characters");
                if (!isNew && users.existsByUsernameIgnoreCase(email))
                    errors.add("Stored employee email is already used by another portal account");
                try { accountRole(existingEmployee); }
                catch (InvalidOperationException ex) { errors.add(ex.getMessage()); }
            }
            if (!row.managerRacfId().isEmpty() && !available.contains(row.managerRacfId())) errors.add("Manager RACF ID not found in employees or file");
            Set<String> visited = new HashSet<>();
            String current = row.racfId();
            while (current != null) {
                if (!visited.add(current)) { errors.add("Self-reporting or reporting cycle detected"); break; }
                current = graph.get(current);
            }
            var existing = links.get(row.racfId());
            String action = existing == null ? "INSERT" : "UPDATE";
            if (existing != null && (row.managerRacfId().isEmpty()
                    || existing.getReportingManager() != null && Objects.equals(
                    key(existing.getReportingManager().getCsxRacfId()), row.managerRacfId()))) action = "UNCHANGED";
            checked.add(new Row(row.rowNumber(), row.racfId(), row.firstName(), row.lastName(), row.email(),
                    row.managerRacfId(), errors.isEmpty() ? action : "ERROR", errors));
        }
        int errors = (int) checked.stream().filter(r -> !r.errors().isEmpty()).count();
        return new EmployeeHierarchyImportResponse(job.getId(), errors == 0 ? "VALIDATED" : "INVALID",
                "Existing employee details are preserved. Blank manager cells keep the current manager. "
                        + "New employees are ACTIVE with generated RIT IDs. Missing portal accounts and employee roles will be created. No invitations will be sent. "
                        + "All rows must be valid before this batch can be committed.",
                (int) checked.stream().filter(r -> r.errors().isEmpty() && !byRacf.containsKey(r.racfId())).count(),
                count(checked, "INSERT"), count(checked, "UPDATE"), count(checked, "UNCHANGED"), errors, checked);
    }

    private int count(List<Row> rows, String action) {
        return (int) rows.stream().filter(r -> action.equals(r.action())).count();
    }

    private List<Row> parse(byte[] bytes) throws IOException {
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            if (workbook.getNumberOfSheets() != 1) throw new InvalidOperationException("Use exactly one worksheet");
            var sheet = workbook.getSheetAt(0);
            if (sheet.getLastRowNum() > 2000) throw new InvalidOperationException("Maximum 2000 data rows");
            var header = sheet.getRow(0);
            if (header == null || header.getLastCellNum() != HEADERS.length) throw new InvalidOperationException("Use the supplied template headers");
            for (int i = 0; i < HEADERS.length; i++) if (!HEADERS[i].equalsIgnoreCase(cell(header, i)))
                throw new InvalidOperationException("Expected column " + (i + 1) + ": " + HEADERS[i]);
            List<Row> result = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                var row = sheet.getRow(i);
                if (row == null) continue;
                if (row.getLastCellNum() > HEADERS.length) throw new InvalidOperationException("Unexpected extra column at row " + (i + 1));
                String[] values = new String[5];
                for (int j = 0; j < 5; j++) values[j] = cell(row, j);
                if (Arrays.stream(values).allMatch(String::isEmpty)) continue;
                result.add(new Row(i + 1, key(values[0]), values[1], values[2],
                        values[3].toLowerCase(Locale.ROOT), key(values[4]), "", List.of()));
            }
            if (result.isEmpty()) throw new InvalidOperationException("The workbook has no data rows");
            return result;
        } catch (InvalidOperationException ex) { throw ex; }
        catch (IOException | RuntimeException ex) { throw new InvalidOperationException("Cannot read workbook; upload a valid, unencrypted .xlsx file"); }
    }

    private String cell(org.apache.poi.ss.usermodel.Row row, int column) {
        var cell = row.getCell(column);
        if (cell == null || cell.getCellType() == CellType.BLANK) return "";
        if (cell.getCellType() != CellType.STRING) throw new InvalidOperationException(
                "Use text cells only; numeric values and formulas are not supported (row " + (row.getRowNum() + 1) + ")");
        return cell.getStringCellValue().trim();
    }

    private String key(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }

    private byte[] workbook(List<String[]> rows) throws IOException {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Hierarchy");
            var style = workbook.createCellStyle();
            style.setDataFormat(workbook.createDataFormat().getFormat("@"));
            for (int i = 0; i < rows.get(0).length; i++) {
                sheet.setDefaultColumnStyle(i, style);
                sheet.setColumnWidth(i, 32 * 256);
            }
            for (int i = 0; i < rows.size(); i++) {
                var row = sheet.createRow(i);
                for (int j = 0; j < rows.get(i).length; j++) row.createCell(j, CellType.STRING).setCellValue(rows.get(i)[j]);
            }
            sheet.createFreezePane(0, 1);
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
