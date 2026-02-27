package com.kys.processes.controller;

import com.kys.processes.entity.DraftProcessStep;
import com.kys.processes.repository.DraftProcessStepRepository;
import com.kys.processes.repository.ProcessRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/processes/{processId}/draft-steps")
@RequiredArgsConstructor
public class DraftProcessStepController {

    private final DraftProcessStepRepository repo;
    private final ProcessRepository processRepo;

    @GetMapping
    public ResponseEntity<?> list(@PathVariable Long processId) {
        if (!processRepo.existsById(processId)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(repo.findByProcessIdOrderByStepNoAsc(processId).stream().map(this::toMap).toList());
    }

    @PostMapping
    public ResponseEntity<?> add(@PathVariable Long processId, @RequestBody Map<String, Object> body) {
        return processRepo.findById(processId).map(proc -> {
            DraftProcessStep s = DraftProcessStep.builder()
                .process(proc)
                .stepNo(body.get("stepNo") != null ? Integer.parseInt(body.get("stepNo").toString()) : nextStepNo(processId))
                .stepName(str(body, "stepName"))
                .trigger(str(body, "trigger"))
                .input(str(body, "input"))
                .workDone(str(body, "workDone"))
                .output(str(body, "output"))
                .transferredRole(str(body, "transferredRole"))
                .responsible(str(body, "responsible"))
                .notes(str(body, "notes"))
                .build();
            return ResponseEntity.ok(toMap(repo.save(s)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{sid}")
    public ResponseEntity<?> update(@PathVariable Long processId, @PathVariable Long sid, @RequestBody Map<String, Object> body) {
        return repo.findById(sid).filter(s -> s.getProcess().getId().equals(processId)).map(s -> {
            if (body.get("stepNo") != null) s.setStepNo(Integer.parseInt(body.get("stepNo").toString()));
            if (body.get("stepName") != null) s.setStepName(str(body, "stepName"));
            if (body.get("trigger") != null) s.setTrigger(str(body, "trigger"));
            if (body.get("input") != null) s.setInput(str(body, "input"));
            if (body.get("workDone") != null) s.setWorkDone(str(body, "workDone"));
            if (body.get("output") != null) s.setOutput(str(body, "output"));
            if (body.get("transferredRole") != null) s.setTransferredRole(str(body, "transferredRole"));
            if (body.get("responsible") != null) s.setResponsible(str(body, "responsible"));
            if (body.get("notes") != null) s.setNotes(str(body, "notes"));
            return ResponseEntity.ok(toMap(repo.save(s)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{sid}")
    public ResponseEntity<?> delete(@PathVariable Long processId, @PathVariable Long sid) {
        return repo.findById(sid).filter(s -> s.getProcess().getId().equals(processId)).map(s -> {
            repo.delete(s);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Taslak Adımlar");
            String[] headers = {"No", "Adım Adı", "Tetikleyici", "Girdi", "Yapılan İş", "Çıktı", "Devredilen Rol", "Sorumlu", "Açıklama"};
            Row headerRow = sheet.createRow(0);
            CellStyle style = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            style.setFont(font);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(style);
                sheet.setColumnWidth(i, 5000);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"taslak-adimlar-sablon.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(out.toByteArray());
        }
    }

    @PostMapping("/import")
    @Transactional
    public ResponseEntity<?> importExcel(@PathVariable Long processId, @RequestParam("file") MultipartFile file) throws IOException {
        return processRepo.findById(processId).map(proc -> {
            try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
                Sheet sheet = wb.getSheetAt(0);
                int maxNo = repo.findByProcessIdOrderByStepNoAsc(processId).stream()
                    .mapToInt(DraftProcessStep::getStepNo).max().orElse(0);
                int counter = 1;
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    String stepName = cellStr(row, 1);
                    if (stepName == null || stepName.isBlank()) continue;
                    int stepNo;
                    String noStr = cellStr(row, 0);
                    if (noStr != null && !noStr.isBlank()) {
                        try { stepNo = Integer.parseInt(noStr.trim()); }
                        catch (NumberFormatException e) { stepNo = maxNo + counter++; }
                    } else {
                        stepNo = maxNo + counter++;
                    }
                    DraftProcessStep s = DraftProcessStep.builder()
                        .process(proc)
                        .stepNo(stepNo)
                        .stepName(stepName)
                        .trigger(cellStr(row, 2))
                        .input(cellStr(row, 3))
                        .workDone(cellStr(row, 4))
                        .output(cellStr(row, 5))
                        .transferredRole(cellStr(row, 6))
                        .responsible(cellStr(row, 7))
                        .notes(cellStr(row, 8))
                        .build();
                    repo.save(s);
                }
                return ResponseEntity.ok(repo.findByProcessIdOrderByStepNoAsc(processId).stream().map(this::toMap).toList());
            } catch (IOException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Excel okunamadı: " + e.getMessage()));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    private int nextStepNo(Long processId) {
        return repo.findByProcessIdOrderByStepNoAsc(processId).stream()
            .mapToInt(DraftProcessStep::getStepNo).max().orElse(0) + 1;
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : "";
    }

    private String cellStr(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private Map<String, Object> toMap(DraftProcessStep s) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", s.getId());
        m.put("stepNo", s.getStepNo());
        m.put("stepName", s.getStepName() != null ? s.getStepName() : "");
        m.put("trigger", s.getTrigger() != null ? s.getTrigger() : "");
        m.put("input", s.getInput() != null ? s.getInput() : "");
        m.put("workDone", s.getWorkDone() != null ? s.getWorkDone() : "");
        m.put("output", s.getOutput() != null ? s.getOutput() : "");
        m.put("transferredRole", s.getTransferredRole() != null ? s.getTransferredRole() : "");
        m.put("responsible", s.getResponsible() != null ? s.getResponsible() : "");
        m.put("notes", s.getNotes() != null ? s.getNotes() : "");
        return m;
    }
}
