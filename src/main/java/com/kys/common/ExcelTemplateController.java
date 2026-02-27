package com.kys.common;

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
import java.util.Map;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class ExcelTemplateController {

    private static final Map<String, List<String>> TEMPLATES = Map.of(
        "units",       List.of("Ad *", "Kod *"),
        "roles",       List.of("Ad *", "Kod *", "Tür (STRATEGIC/MANAGERIAL/OPERATIONAL)", "Açıklama"),
        "titles",      List.of("Ad *", "Kod *"),
        "positions",   List.of("Ad *", "Kod *", "Seviye (GOVERNANCE/EXECUTIVE/MANAGER/STAFF)", "Açıklama"),
        "doc-types",   List.of("Kod *", "Ad *", "Seviye (1-5)", "Açıklama"),
        "doc-statuses",List.of("Kod *", "Ad *", "Sıra"),
        "documents",   List.of("Kod *", "Başlık *", "Doküman Türü Kodu", "Durum Kodu", "Üst Doküman Kodu"),
        "users",       List.of("Ad *", "Soyad *", "Sicil No", "Telefon", "E-posta", "İşe Giriş Tarihi (YYYY-MM-DD)", "Birim Kodu", "Pozisyon Kodu", "Ünvan Kodu"),
        "processes",   List.of("Kod *", "Ad *", "Seviye (L1/L2/L3)", "Tür (CORE/SUPPORT/MANAGEMENT)",
                               "Kritiklik (HIGH/MEDIUM/LOW)", "Üst Süreç Kodu", "Sorumlu Rol Kodu", "Kısa Açıklama")
    );

    private static final Map<String, String[][]> SAMPLES = Map.of(
        "units",       new String[][]{{"Genel Müdürlük", "GM"}, {"İnsan Kaynakları", "IK"}},
        "roles",       new String[][]{{"Genel Müdür", "GM", "STRATEGIC", "Üst yönetim"}, {"IK Müdürü", "IKM", "MANAGERIAL", ""}},
        "titles",      new String[][]{{"Dr.", "DR"}, {"Prof.", "PROF"}, {"Uzman", "UZM"}},
        "positions",   new String[][]{{"Müdür", "MDR", "MANAGER", ""}, {"Uzman", "UZM", "STAFF", ""}},
        "doc-types",   new String[][]{{"POL", "Politika", "1", ""}, {"PRO", "Prosedür", "2", ""}, {"TAL", "Talimat", "3", ""}},
        "doc-statuses",new String[][]{{"TASLAK", "Taslak", "1"}, {"GOZDEN", "Gözden Geçirildi", "2"}, {"ONAYLANDI", "Onaylandı", "3"}},
        "documents",   new String[][]{{"POL-001", "Kalite Politikası", "POL", "TASLAK", ""}, {"PRO-001", "Satın Alma Prosedürü", "PRO", "ONAYLANDI", ""}},
        "users",       new String[][]{{"Ali", "Yılmaz", "12345", "05551234567", "ali@sirket.com", "2020-01-15", "IK", "MDR", "DR"}, {"Ayşe", "Demir", "12346", "", "", "2021-06-01", "GM", "UZM", "PROF"}},
        "processes",   new String[][]{{"SP-001", "Satın Alma Süreci", "L1", "CORE", "HIGH", "", "GM", "Mal ve hizmet tedariki"}, {"SP-002", "İK Yönetimi", "L1", "SUPPORT", "MEDIUM", "", "IKM", ""}}
    );

    @GetMapping("/{type}")
    public ResponseEntity<byte[]> download(@PathVariable String type) throws IOException {
        List<String> headers = TEMPLATES.get(type);
        if (headers == null) return ResponseEntity.notFound().build();

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Veri");

            // Başlık stili
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = wb.createFont();
            font.setColor(IndexedColors.WHITE.getIndex());
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // Başlık satırı
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 6000);
            }

            // Örnek veriler
            String[][] samples = SAMPLES.get(type);
            if (samples != null) {
                CellStyle sampleStyle = wb.createCellStyle();
                sampleStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
                sampleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                for (int r = 0; r < samples.length; r++) {
                    Row row = sheet.createRow(r + 1);
                    for (int c = 0; c < samples[r].length; c++) {
                        Cell cell = row.createCell(c);
                        cell.setCellValue(samples[r][c]);
                        cell.setCellStyle(sampleStyle);
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + type + "_sablon.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(out.toByteArray());
        }
    }
}
