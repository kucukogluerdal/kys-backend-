package com.kys.common;

import com.kys.dms.repository.DocumentRepository;
import com.kys.dms.repository.DocumentStatusRepository;
import com.kys.dms.repository.DocumentTypeRepository;
import com.kys.organization.repository.OrgUnitRepository;
import com.kys.organization.repository.PositionRepository;
import com.kys.organization.repository.RoleRepository;
import com.kys.organization.repository.TitleRepository;
import com.kys.organization.repository.UserProfileRepository;
import com.kys.processes.repository.ProcessRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExcelExportController {

    private final DocumentRepository documentRepo;
    private final OrgUnitRepository unitRepo;
    private final RoleRepository roleRepo;
    private final TitleRepository titleRepo;
    private final PositionRepository positionRepo;
    private final DocumentTypeRepository docTypeRepo;
    private final DocumentStatusRepository docStatusRepo;
    private final UserProfileRepository userProfileRepo;
    private final ProcessRepository processRepo;

    @GetMapping("/units")
    public ResponseEntity<byte[]> exportUnits() throws IOException {
        var headers = List.of("Ad *", "Kod *");
        var rows = unitRepo.findAll().stream()
            .map(u -> new String[]{u.getName(), u.getCode()})
            .toList();
        return buildExcel("units_export", headers, rows);
    }

    @GetMapping("/roles")
    public ResponseEntity<byte[]> exportRoles() throws IOException {
        var headers = List.of("Ad *", "Kod *", "Tür (STRATEGIC/MANAGERIAL/OPERATIONAL)", "Açıklama");
        var rows = roleRepo.findAll().stream()
            .map(r -> new String[]{r.getName(), r.getCode(),
                r.getRoleType() != null ? r.getRoleType().name() : "",
                r.getDescription() != null ? r.getDescription() : ""})
            .toList();
        return buildExcel("roles_export", headers, rows);
    }

    @GetMapping("/titles")
    public ResponseEntity<byte[]> exportTitles() throws IOException {
        var headers = List.of("Ad *", "Kod *");
        var rows = titleRepo.findAll().stream()
            .map(t -> new String[]{t.getName(), t.getCode()})
            .toList();
        return buildExcel("titles_export", headers, rows);
    }

    @GetMapping("/positions")
    public ResponseEntity<byte[]> exportPositions() throws IOException {
        var headers = List.of("Ad *", "Kod *", "Seviye (MANAGER/STAFF)", "Açıklama");
        var rows = positionRepo.findAll().stream()
            .map(p -> new String[]{p.getName(), p.getCode(),
                p.getLevel() != null ? p.getLevel().name() : "",
                p.getDescription() != null ? p.getDescription() : ""})
            .toList();
        return buildExcel("positions_export", headers, rows);
    }

    @GetMapping("/doc-types")
    public ResponseEntity<byte[]> exportDocTypes() throws IOException {
        var headers = List.of("Kod *", "Ad *", "Seviye (1-5)", "Açıklama");
        var rows = docTypeRepo.findAll().stream()
            .map(t -> new String[]{t.getCode(), t.getName(),
                String.valueOf(t.getLevel()),
                t.getDescription() != null ? t.getDescription() : ""})
            .toList();
        return buildExcel("doc-types_export", headers, rows);
    }

    @GetMapping("/doc-statuses")
    public ResponseEntity<byte[]> exportDocStatuses() throws IOException {
        var headers = List.of("Kod *", "Ad *", "Sıra");
        var rows = docStatusRepo.findAll().stream()
            .map(s -> new String[]{s.getCode(), s.getName(), String.valueOf(s.getOrder())})
            .toList();
        return buildExcel("doc-statuses_export", headers, rows);
    }

    @GetMapping("/users")
    public ResponseEntity<byte[]> exportUsers() throws IOException {
        var headers = List.of("Ad *", "Soyad *", "Sicil No", "Telefon", "E-posta", "İşe Giriş Tarihi (YYYY-MM-DD)", "Birim Kodu", "Pozisyon Kodu", "Ünvan Kodu");
        var rows = userProfileRepo.findAll().stream()
            .map(p -> new String[]{
                p.getFirstName()  != null ? p.getFirstName()  : "",
                p.getLastName()   != null ? p.getLastName()   : "",
                p.getEmployeeId() != null ? p.getEmployeeId() : "",
                p.getPhone()      != null ? p.getPhone()      : "",
                p.getEmail()      != null ? p.getEmail()      : "",
                p.getHireDate()   != null ? p.getHireDate().toString() : "",
                p.getOrgUnit()    != null ? p.getOrgUnit().getCode()  : "",
                p.getPosition()   != null ? p.getPosition().getCode() : "",
                p.getTitle()      != null ? p.getTitle().getCode()    : ""})
            .toList();
        return buildExcel("users_export", headers, rows);
    }

    @GetMapping("/documents")
    public ResponseEntity<byte[]> exportDocuments() throws IOException {
        var headers = List.of("Kod *", "Başlık *", "Doküman Türü Kodu", "Durum Kodu", "Üst Doküman Kodu");
        var rows = documentRepo.findAll().stream()
            .map(d -> new String[]{
                d.getCode(),
                d.getTitle(),
                d.getDocType() != null ? d.getDocType().getCode() : "",
                d.getStatus() != null ? d.getStatus().getCode() : "",
                d.getParent() != null ? d.getParent().getCode() : ""})
            .toList();
        return buildExcel("documents_export", headers, rows);
    }

    @GetMapping("/processes")
    public ResponseEntity<byte[]> exportProcesses() throws IOException {
        var headers = List.of("Kod *", "Ad *", "Seviye (L1/L2/L3)", "Tür (CORE/SUPPORT/MANAGEMENT)",
            "Kritiklik (HIGH/MEDIUM/LOW)", "Üst Süreç Kodu", "Sorumlu Rol Kodu", "Kısa Açıklama");
        var rows = processRepo.findAll().stream()
            .map(p -> new String[]{
                p.getCode(),
                p.getName(),
                p.getLevel() != null ? p.getLevel().name() : "",
                p.getProcessType() != null ? p.getProcessType().name() : "",
                p.getCriticality() != null ? p.getCriticality().name() : "",
                p.getParent() != null ? p.getParent().getCode() : "",
                p.getOwnerRole() != null ? p.getOwnerRole().getCode() : "",
                p.getShortDescription() != null ? p.getShortDescription() : ""})
            .toList();
        return buildExcel("processes_export", headers, rows);
    }

    private ResponseEntity<byte[]> buildExcel(String filename, List<String> headers, List<String[]> rows) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Veri");

            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = wb.createFont();
            font.setColor(IndexedColors.WHITE.getIndex());
            font.setBold(true);
            headerStyle.setFont(font);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 6000);
            }

            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                String[] rowData = rows.get(r);
                for (int c = 0; c < rowData.length; c++) {
                    row.createCell(c).setCellValue(rowData[c]);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(out.toByteArray());
        }
    }
}
