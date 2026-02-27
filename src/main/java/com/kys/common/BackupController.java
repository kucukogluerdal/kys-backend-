package com.kys.common;

import com.kys.organization.entity.*;
import com.kys.organization.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
public class BackupController {

    private final OrgUnitRepository   unitRepo;
    private final RoleRepository      roleRepo;
    private final TitleRepository     titleRepo;
    private final PositionRepository  positionRepo;
    private final UserProfileRepository userProfileRepo;
    private final EntityManager       em;

    // ─── İNDİR ──────────────────────────────────────────────────
    @GetMapping("/download")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> download() throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle hs = headerStyle(wb);

            // 1. Birimler
            Sheet s1 = wb.createSheet("Birimler");
            writeHeaders(s1.createRow(0), hs, "Ad", "Kod", "Üst Birim Kodu", "Aktif");
            int r = 1;
            for (OrgUnit u : unitRepo.findAll()) {
                writeRow(s1.createRow(r++),
                    u.getName(), u.getCode(),
                    u.getParent() != null ? u.getParent().getCode() : "",
                    u.isActive() ? "EVET" : "HAYIR");
            }
            autoWidth(s1, 4);

            // 2. Roller
            Sheet s2 = wb.createSheet("Roller");
            writeHeaders(s2.createRow(0), hs, "Ad", "Kod", "Tür", "Açıklama", "Aktif");
            r = 1;
            for (Role ro : roleRepo.findAll()) {
                writeRow(s2.createRow(r++),
                    ro.getName(), ro.getCode(),
                    ro.getRoleType() != null ? ro.getRoleType().name() : "",
                    ro.getDescription() != null ? ro.getDescription() : "",
                    ro.isActive() ? "EVET" : "HAYIR");
            }
            autoWidth(s2, 5);

            // 3. Ünvanlar
            Sheet s3 = wb.createSheet("Unvanlar");
            writeHeaders(s3.createRow(0), hs, "Ad", "Kod", "Aktif");
            r = 1;
            for (Title t : titleRepo.findAll()) {
                writeRow(s3.createRow(r++),
                    t.getName(), t.getCode(),
                    t.isActive() ? "EVET" : "HAYIR");
            }
            autoWidth(s3, 3);

            // 4. Pozisyonlar (birim + rol atamaları dahil)
            Sheet s4 = wb.createSheet("Pozisyonlar");
            writeHeaders(s4.createRow(0), hs,
                "Ad", "Kod", "Seviye", "Açıklama", "Atanmış Birimler", "Atanmış Roller", "Aktif");
            r = 1;
            for (Position p : positionRepo.findAll()) {
                String unitCodes = unitRepo.findByPositionsId(p.getId()).stream()
                    .map(OrgUnit::getCode).sorted().collect(Collectors.joining(","));
                String roleCodes = p.getRoles().stream()
                    .map(Role::getCode).sorted().collect(Collectors.joining(","));
                writeRow(s4.createRow(r++),
                    p.getName(), p.getCode(),
                    p.getLevel() != null ? p.getLevel().name() : "",
                    p.getDescription() != null ? p.getDescription() : "",
                    unitCodes, roleCodes,
                    p.isActive() ? "EVET" : "HAYIR");
            }
            autoWidth(s4, 7);

            // 5. Personel
            Sheet s5 = wb.createSheet("Personel");
            writeHeaders(s5.createRow(0), hs,
                "Ad", "Soyad", "Sicil No", "Telefon", "E-posta",
                "İşe Giriş Tarihi", "Birim Kodu", "Pozisyon Kodu", "Ünvan Kodu", "Aktif");
            r = 1;
            for (UserProfile up : userProfileRepo.findAll()) {
                writeRow(s5.createRow(r++),
                    up.getFirstName()  != null ? up.getFirstName()  : "",
                    up.getLastName()   != null ? up.getLastName()   : "",
                    up.getEmployeeId() != null ? up.getEmployeeId() : "",
                    up.getPhone()      != null ? up.getPhone()      : "",
                    up.getEmail()      != null ? up.getEmail()      : "",
                    up.getHireDate()   != null ? up.getHireDate().toString() : "",
                    up.getOrgUnit()    != null ? up.getOrgUnit().getCode()   : "",
                    up.getPosition()   != null ? up.getPosition().getCode()  : "",
                    up.getTitle()      != null ? up.getTitle().getCode()     : "",
                    up.isActive()      ? "EVET" : "HAYIR");
            }
            autoWidth(s5, 10);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            String filename = "kys_yedek_" + LocalDate.now() + ".xlsx";
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(out.toByteArray());
        }
    }

    // ─── GERİ YÜKLE ─────────────────────────────────────────────
    @PostMapping("/restore")
    @Transactional
    public ResponseEntity<?> restore(@RequestParam("file") MultipartFile file) throws IOException {
        Map<String, Integer> counts = new LinkedHashMap<>();

        try (Workbook wb = org.apache.poi.ss.usermodel.WorkbookFactory.create(file.getInputStream())) {

            // 1. Birimler (first pass: create/update without parent)
            Sheet s1 = wb.getSheet("Birimler");
            int cnt = 0;
            if (s1 != null) {
                for (int i = 1; i <= s1.getLastRowNum(); i++) {
                    Row row = s1.getRow(i); if (row == null) continue;
                    String name = str(row, 0), code = str(row, 1);
                    if (name.isBlank() || code.isBlank()) continue;
                    OrgUnit u = unitRepo.findByCode(code).orElse(new OrgUnit());
                    u.setName(name); u.setCode(code);
                    u.setActive(!"HAYIR".equalsIgnoreCase(str(row, 3)));
                    unitRepo.save(u); cnt++;
                }
                unitRepo.flush();
                // Second pass: link parents
                for (int i = 1; i <= s1.getLastRowNum(); i++) {
                    Row row = s1.getRow(i); if (row == null) continue;
                    String code = str(row, 1), parentCode = str(row, 2);
                    if (code.isBlank() || parentCode.isBlank()) continue;
                    unitRepo.findByCode(code).ifPresent(u ->
                        unitRepo.findByCode(parentCode).ifPresent(parent -> {
                            u.setParent(parent); unitRepo.save(u);
                        }));
                }
            }
            counts.put("birimler", cnt);

            // 2. Roller
            Sheet s2 = wb.getSheet("Roller");
            cnt = 0;
            if (s2 != null) {
                for (int i = 1; i <= s2.getLastRowNum(); i++) {
                    Row row = s2.getRow(i); if (row == null) continue;
                    String name = str(row, 0), code = str(row, 1), type = str(row, 2);
                    if (name.isBlank() || code.isBlank()) continue;
                    Role ro = roleRepo.findByCode(code).orElse(new Role());
                    ro.setName(name); ro.setCode(code);
                    ro.setDescription(str(row, 3));
                    ro.setActive(!"HAYIR".equalsIgnoreCase(str(row, 4)));
                    try { ro.setRoleType(Role.RoleType.valueOf(type.toUpperCase())); }
                    catch (Exception e) { ro.setRoleType(Role.RoleType.OPERATIONAL); }
                    roleRepo.save(ro); cnt++;
                }
                roleRepo.flush();
            }
            counts.put("roller", cnt);

            // 3. Ünvanlar
            Sheet s3 = wb.getSheet("Unvanlar");
            cnt = 0;
            if (s3 != null) {
                for (int i = 1; i <= s3.getLastRowNum(); i++) {
                    Row row = s3.getRow(i); if (row == null) continue;
                    String name = str(row, 0), code = str(row, 1);
                    if (name.isBlank() || code.isBlank()) continue;
                    Title t = titleRepo.findByCode(code).orElse(new Title());
                    t.setName(name); t.setCode(code);
                    t.setActive(!"HAYIR".equalsIgnoreCase(str(row, 2)));
                    titleRepo.save(t); cnt++;
                }
                titleRepo.flush();
            }
            counts.put("unvanlar", cnt);

            // 4. Pozisyonlar (with role assignments via collection, unit assignments via native SQL)
            Sheet s4 = wb.getSheet("Pozisyonlar");
            cnt = 0;
            if (s4 != null) {
                for (int i = 1; i <= s4.getLastRowNum(); i++) {
                    Row row = s4.getRow(i); if (row == null) continue;
                    String name      = str(row, 0), code     = str(row, 1);
                    String level     = str(row, 2), desc     = str(row, 3);
                    String unitCodes = str(row, 4), roleCodes = str(row, 5);
                    if (name.isBlank() || code.isBlank()) continue;

                    Position p = positionRepo.findByCode(code).orElse(new Position());
                    p.setName(name); p.setCode(code); p.setDescription(desc);
                    p.setActive(!"HAYIR".equalsIgnoreCase(str(row, 6)));
                    try { p.setLevel(Position.Level.valueOf(level.toUpperCase())); }
                    catch (Exception e) { p.setLevel(Position.Level.STAFF); }

                    // Rol atamaları (Position owns this collection)
                    p.getRoles().clear();
                    if (!roleCodes.isBlank()) {
                        for (String rc : roleCodes.split(",")) {
                            roleRepo.findByCode(rc.trim()).ifPresent(ro -> p.getRoles().add(ro));
                        }
                    }
                    Position saved = positionRepo.save(p);
                    positionRepo.flush();

                    // Birim-Pozisyon atamaları (OrgUnit owns this via unit_positions table)
                    em.createNativeQuery("DELETE FROM unit_positions WHERE position_id = ?1")
                        .setParameter(1, saved.getId()).executeUpdate();
                    if (!unitCodes.isBlank()) {
                        for (String uc : unitCodes.split(",")) {
                            unitRepo.findByCode(uc.trim()).ifPresent(u ->
                                em.createNativeQuery(
                                    "INSERT INTO unit_positions (unit_id, position_id) VALUES (?1, ?2)")
                                    .setParameter(1, u.getId())
                                    .setParameter(2, saved.getId())
                                    .executeUpdate());
                        }
                    }
                    cnt++;
                }
            }
            counts.put("pozisyonlar", cnt);

            // 5. Personel
            Sheet s5 = wb.getSheet("Personel");
            cnt = 0;
            if (s5 != null) {
                for (int i = 1; i <= s5.getLastRowNum(); i++) {
                    Row row = s5.getRow(i); if (row == null) continue;
                    String firstName  = str(row, 0), lastName   = str(row, 1);
                    String employeeId = str(row, 2), phone      = str(row, 3);
                    String email      = str(row, 4), hireDate   = str(row, 5);
                    String unitCode   = str(row, 6), posCode    = str(row, 7);
                    String titleCode  = str(row, 8);
                    if (firstName.isBlank() && lastName.isBlank()) continue;

                    UserProfile up;
                    if (!employeeId.isBlank() && userProfileRepo.existsByEmployeeId(employeeId)) {
                        up = userProfileRepo.findByEmployeeId(employeeId).get();
                    } else {
                        up = new UserProfile();
                    }
                    up.setFirstName(firstName); up.setLastName(lastName);
                    up.setFullName((firstName + " " + lastName).trim());
                    if (!employeeId.isBlank()) up.setEmployeeId(employeeId);
                    if (!phone.isBlank())      up.setPhone(phone);
                    if (!email.isBlank())      up.setEmail(email);
                    if (!hireDate.isBlank()) {
                        try { up.setHireDate(LocalDate.parse(hireDate)); } catch (Exception ignored) {}
                    }
                    up.setActive(!"HAYIR".equalsIgnoreCase(str(row, 9)));
                    if (!unitCode.isBlank())  unitRepo.findByCode(unitCode).ifPresent(up::setOrgUnit);
                    if (!posCode.isBlank())   positionRepo.findByCode(posCode).ifPresent(up::setPosition);
                    if (!titleCode.isBlank()) titleRepo.findByCode(titleCode).ifPresent(up::setTitle);
                    userProfileRepo.save(up); cnt++;
                }
            }
            counts.put("personel", cnt);
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Yedek başarıyla geri yüklendi",
            "restored", counts
        ));
    }

    // ─── Yardımcılar ────────────────────────────────────────────
    private String str(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell))
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                yield String.valueOf((long) cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> "";
        };
    }

    private void writeHeaders(Row row, CellStyle style, String... headers) {
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void writeRow(Row row, String... values) {
        for (int i = 0; i < values.length; i++)
            row.createCell(i).setCellValue(values[i]);
    }

    private void autoWidth(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) sheet.setColumnWidth(i, 5500);
    }

    private CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = wb.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}
