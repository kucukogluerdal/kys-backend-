package com.kys.processes.controller;

import com.kys.processes.entity.Process;
import com.kys.processes.entity.ProcessIO;
import com.kys.processes.entity.ProcessParty;
import com.kys.processes.entity.ProcessStakeholder;
import com.kys.processes.entity.ProcessStep;
import com.kys.processes.repository.*;
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
import java.util.*;

@RestController
@RequestMapping("/api/process-card")
@RequiredArgsConstructor
public class ProcessCardController {

    private final ProcessRepository processRepo;
    private final ProcessIORepository ioRepo;
    private final ProcessPartyRepository partyRepo;
    private final ProcessStakeholderRepository stakeholderRepo;
    private final ProcessStepRepository stepRepo;

    // ─── Export ──────────────────────────────────────────────────

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long id) throws IOException {
        Process p = processRepo.findById(id).orElseThrow();
        byte[] bytes = buildWorkbook(p, false);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + p.getCode() + "-kimlik-karti.xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes);
    }

    // ─── Template ────────────────────────────────────────────────

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() throws IOException {
        byte[] bytes = buildWorkbook(null, true);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"surec-kimlik-karti-sablonu.xlsx\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes);
    }

    // ─── Import ──────────────────────────────────────────────────

    @PostMapping("/import")
    @Transactional
    public ResponseEntity<?> importCard(@RequestParam("file") MultipartFile file) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(file.getInputStream())) {

            // 1. "Kimlik" sheet'inden skalar alanları oku
            Sheet kimlik = wb.getSheet("Kimlik");
            if (kimlik == null) return ResponseEntity.badRequest().body(Map.of("message", "'Kimlik' sheet bulunamadı"));

            Map<String, String> kv = new LinkedHashMap<>();
            for (Row row : kimlik) {
                if (row == null) continue;
                String key = cellStr(row.getCell(0));
                String val = cellStr(row.getCell(1));
                if (!key.isEmpty()) kv.put(key, val);
            }

            String code = kv.get("Süreç Kodu");
            if (code == null || code.isBlank())
                return ResponseEntity.badRequest().body(Map.of("message", "Süreç kodu bulunamadı"));

            Process p = processRepo.findByCode(code.trim())
                .orElseGet(() -> { Process np = new Process(); np.setCode(code.trim()); np.setName(code.trim()); return np; });

            applyKv(p, kv);
            processRepo.save(p);

            // 2. Girdi-Çıktı sheet
            Sheet ioSheet = wb.getSheet("Girdi-Cikti");
            if (ioSheet != null) appendIOs(p, ioSheet);

            // 3. Tedarikci-Musteri sheet
            Sheet partySheet = wb.getSheet("Tedarikci-Musteri");
            if (partySheet != null) appendParties(p, partySheet);

            // 4. Paydaşlar sheet
            Sheet shSheet = wb.getSheet("Paydaşlar");
            if (shSheet != null) appendStakeholders(p, shSheet);

            // 5. Adımlar sheet
            Sheet stepSheet = wb.getSheet("Adımlar");
            if (stepSheet != null) appendSteps(p, stepSheet);

            return ResponseEntity.ok(Map.of("message", "Import başarılı", "processId", p.getId(), "code", p.getCode()));
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private byte[] buildWorkbook(Process p, boolean template) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Stil: kalın başlık
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Sheet 1: Kimlik
            Sheet kimlik = wb.createSheet("Kimlik");
            kimlik.setColumnWidth(0, 8000);
            kimlik.setColumnWidth(1, 16000);
            String[][] rows = {
                {"Süreç Kodu",          template ? "" : nvl(p.getCode())},
                {"Süreç Adı",           template ? "" : nvl(p.getName())},
                {"Seviye",              template ? "" : (p != null && p.getLevel() != null ? p.getLevel().name() : "")},
                {"Tür",                 template ? "" : (p != null && p.getProcessType() != null ? p.getProcessType().name() : "")},
                {"Kritiklik",           template ? "" : (p != null && p.getCriticality() != null ? p.getCriticality().name() : "")},
                {"Durum",               template ? "" : (p != null && p.getStatus() != null ? p.getStatus().name() : "")},
                {"Sürecin Amacı",       template ? "" : nvl(p != null ? p.getPurpose() : null)},
                {"Stratejik Amaç",      template ? "" : nvl(p != null ? p.getStrategicGoal() : null)},
                {"Stratejik Hedef",     template ? "" : nvl(p != null ? p.getStrategicTarget() : null)},
                {"Başlangıç Noktası",   template ? "" : nvl(p != null ? p.getStartPoint() : null)},
                {"Bitiş Noktası",       template ? "" : nvl(p != null ? p.getEndPoint() : null)},
                {"Kapsam",              template ? "" : nvl(p != null ? p.getProcessScope() : null)},
                {"Etkilendiği Süreçler",template ? "" : nvl(p != null ? p.getAffectedBy() : null)},
                {"Etkilediği Süreçler", template ? "" : nvl(p != null ? p.getAffects() : null)},
                {"Kısa Açıklama",       template ? "" : nvl(p != null ? p.getShortDescription() : null)},
                {"Açıklama/Notlar",     template ? "" : nvl(p != null ? p.getDescription() : null)},
            };
            // Başlık satırı
            Row header = kimlik.createRow(0);
            createBoldCell(header, 0, "Alan", headerStyle);
            createBoldCell(header, 1, "Değer", headerStyle);
            for (int i = 0; i < rows.length; i++) {
                Row row = kimlik.createRow(i + 1);
                row.createCell(0).setCellValue(rows[i][0]);
                row.createCell(1).setCellValue(rows[i][1]);
            }

            // Sheet 2: Girdi-Cikti
            Sheet ioSheet = wb.createSheet("Girdi-Cikti");
            ioSheet.setColumnWidth(0, 5000);
            ioSheet.setColumnWidth(1, 14000);
            Row ioHeader = ioSheet.createRow(0);
            createBoldCell(ioHeader, 0, "Tür (INPUT/OUTPUT)", headerStyle);
            createBoldCell(ioHeader, 1, "Ad", headerStyle);
            if (!template && p != null) {
                List<ProcessIO> ios = ioRepo.findByProcessId(p.getId());
                for (int i = 0; i < ios.size(); i++) {
                    Row r = ioSheet.createRow(i + 1);
                    r.createCell(0).setCellValue(ios.get(i).getIoType().name());
                    r.createCell(1).setCellValue(ios.get(i).getName());
                }
            }

            // Sheet 3: Tedarikci-Musteri
            Sheet partySheet = wb.createSheet("Tedarikci-Musteri");
            partySheet.setColumnWidth(0, 5000);
            partySheet.setColumnWidth(1, 14000);
            Row partyHeader = partySheet.createRow(0);
            createBoldCell(partyHeader, 0, "Tür (SUPPLIER/CUSTOMER)", headerStyle);
            createBoldCell(partyHeader, 1, "Ad", headerStyle);
            if (!template && p != null) {
                List<ProcessParty> parties = partyRepo.findByProcessId(p.getId());
                for (int i = 0; i < parties.size(); i++) {
                    Row r = partySheet.createRow(i + 1);
                    r.createCell(0).setCellValue(parties.get(i).getPartyType().name());
                    r.createCell(1).setCellValue(parties.get(i).getName());
                }
            }

            // Sheet 4: Paydaşlar
            Sheet shSheet = wb.createSheet("Paydaşlar");
            shSheet.setColumnWidth(0, 5000);
            shSheet.setColumnWidth(1, 14000);
            Row shHeader = shSheet.createRow(0);
            createBoldCell(shHeader, 0, "Tür (INTERNAL/EXTERNAL)", headerStyle);
            createBoldCell(shHeader, 1, "Ad", headerStyle);
            if (!template && p != null) {
                List<ProcessStakeholder> shs = stakeholderRepo.findByProcessId(p.getId());
                for (int i = 0; i < shs.size(); i++) {
                    Row r = shSheet.createRow(i + 1);
                    r.createCell(0).setCellValue(shs.get(i).getStakeholderType().name());
                    r.createCell(1).setCellValue(shs.get(i).getName());
                }
            }

            // Sheet 5: Adımlar
            Sheet stepSheet = wb.createSheet("Adımlar");
            stepSheet.setColumnWidth(0, 3000);
            stepSheet.setColumnWidth(1, 10000);
            stepSheet.setColumnWidth(2, 20000);
            Row stepHeader = stepSheet.createRow(0);
            createBoldCell(stepHeader, 0, "No", headerStyle);
            createBoldCell(stepHeader, 1, "Ad", headerStyle);
            createBoldCell(stepHeader, 2, "Açıklama", headerStyle);
            if (!template && p != null) {
                List<ProcessStep> steps = stepRepo.findByProcessIdOrderByStepNoAsc(p.getId());
                for (int i = 0; i < steps.size(); i++) {
                    Row r = stepSheet.createRow(i + 1);
                    r.createCell(0).setCellValue(steps.get(i).getStepNo());
                    r.createCell(1).setCellValue(steps.get(i).getName());
                    r.createCell(2).setCellValue(nvl(steps.get(i).getDescription()));
                }
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    private void createBoldCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void applyKv(Process p, Map<String, String> kv) {
        if (kv.containsKey("Süreç Adı") && !kv.get("Süreç Adı").isBlank()) p.setName(kv.get("Süreç Adı"));
        if (kv.containsKey("Seviye") && !kv.get("Seviye").isBlank()) {
            try { p.setLevel(Process.Level.valueOf(kv.get("Seviye").trim().toUpperCase())); } catch (Exception ignored) {}
        }
        if (kv.containsKey("Tür") && !kv.get("Tür").isBlank()) {
            try { p.setProcessType(Process.ProcessType.valueOf(kv.get("Tür").trim().toUpperCase())); } catch (Exception ignored) {}
        }
        if (kv.containsKey("Kritiklik") && !kv.get("Kritiklik").isBlank()) {
            try { p.setCriticality(Process.Criticality.valueOf(kv.get("Kritiklik").trim().toUpperCase())); } catch (Exception ignored) {}
        }
        if (kv.containsKey("Durum") && !kv.get("Durum").isBlank()) {
            try { p.setStatus(Process.Status.valueOf(kv.get("Durum").trim().toUpperCase())); } catch (Exception ignored) {}
        }
        if (kv.containsKey("Sürecin Amacı")) p.setPurpose(kv.get("Sürecin Amacı"));
        if (kv.containsKey("Stratejik Amaç")) p.setStrategicGoal(kv.get("Stratejik Amaç"));
        if (kv.containsKey("Stratejik Hedef")) p.setStrategicTarget(kv.get("Stratejik Hedef"));
        if (kv.containsKey("Başlangıç Noktası")) p.setStartPoint(kv.get("Başlangıç Noktası"));
        if (kv.containsKey("Bitiş Noktası")) p.setEndPoint(kv.get("Bitiş Noktası"));
        if (kv.containsKey("Kapsam")) p.setProcessScope(kv.get("Kapsam"));
        if (kv.containsKey("Etkilendiği Süreçler")) p.setAffectedBy(kv.get("Etkilendiği Süreçler"));
        if (kv.containsKey("Etkilediği Süreçler")) p.setAffects(kv.get("Etkilediği Süreçler"));
        if (kv.containsKey("Kısa Açıklama")) p.setShortDescription(kv.get("Kısa Açıklama"));
        if (kv.containsKey("Açıklama/Notlar")) p.setDescription(kv.get("Açıklama/Notlar"));
    }

    private void appendIOs(Process p, Sheet sheet) {
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String type = cellStr(row.getCell(0)).trim().toUpperCase();
            String name = cellStr(row.getCell(1)).trim();
            if (name.isEmpty()) continue;
            try {
                ProcessIO io = new ProcessIO();
                io.setProcess(p);
                io.setIoType(ProcessIO.IOType.valueOf(type));
                io.setName(name);
                io.setSortOrder(i);
                ioRepo.save(io);
            } catch (Exception ignored) {}
        }
    }

    private void appendParties(Process p, Sheet sheet) {
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String type = cellStr(row.getCell(0)).trim().toUpperCase();
            String name = cellStr(row.getCell(1)).trim();
            if (name.isEmpty()) continue;
            try {
                ProcessParty party = new ProcessParty();
                party.setProcess(p);
                party.setPartyType(ProcessParty.PartyType.valueOf(type));
                party.setName(name);
                partyRepo.save(party);
            } catch (Exception ignored) {}
        }
    }

    private void appendStakeholders(Process p, Sheet sheet) {
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String type = cellStr(row.getCell(0)).trim().toUpperCase();
            String name = cellStr(row.getCell(1)).trim();
            if (name.isEmpty()) continue;
            try {
                ProcessStakeholder sh = new ProcessStakeholder();
                sh.setProcess(p);
                sh.setStakeholderType(ProcessStakeholder.StakeholderType.valueOf(type));
                sh.setName(name);
                stakeholderRepo.save(sh);
            } catch (Exception ignored) {}
        }
    }

    private void appendSteps(Process p, Sheet sheet) {
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String noStr = cellStr(row.getCell(0)).trim();
            String name = cellStr(row.getCell(1)).trim();
            if (name.isEmpty()) continue;
            try {
                ProcessStep step = new ProcessStep();
                step.setProcess(p);
                step.setStepNo(noStr.isEmpty() ? i : (int) Double.parseDouble(noStr));
                step.setName(name);
                step.setDescription(cellStr(row.getCell(2)));
                stepRepo.save(step);
            } catch (Exception ignored) {}
        }
    }

    private String cellStr(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                yield d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> "";
        };
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
