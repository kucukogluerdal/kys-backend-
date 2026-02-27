package com.kys.common;

import com.kys.dms.entity.Document;
import com.kys.dms.entity.DocumentStatus;
import com.kys.dms.entity.DocumentType;
import com.kys.dms.repository.DocumentRepository;
import com.kys.dms.repository.DocumentStatusRepository;
import com.kys.dms.repository.DocumentTypeRepository;
import com.kys.organization.entity.OrgUnit;
import com.kys.organization.entity.Position;
import com.kys.organization.entity.Role;
import com.kys.organization.entity.Title;
import com.kys.organization.entity.UserProfile;
import com.kys.organization.repository.OrgUnitRepository;
import com.kys.organization.repository.PositionRepository;
import com.kys.organization.repository.RoleRepository;
import com.kys.organization.repository.TitleRepository;
import com.kys.organization.repository.UserProfileRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kys.processes.controller.ProcessCodeConfigController;
import com.kys.processes.entity.Process;
import com.kys.processes.entity.ProcessCodeConfig;
import com.kys.processes.entity.ProcessIO;
import com.kys.processes.entity.ProcessParty;
import com.kys.processes.entity.ProcessStep;
import com.kys.processes.repository.ProcessCodeConfigRepository;
import com.kys.processes.repository.ProcessIORepository;
import com.kys.processes.repository.ProcessLevelPatternRepository;
import com.kys.processes.repository.ProcessPartyRepository;
import com.kys.processes.repository.ProcessRepository;
import com.kys.processes.repository.ProcessStepRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.HashSet;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ExcelImportController {

    private final OrgUnitRepository unitRepo;
    private final RoleRepository roleRepo;
    private final TitleRepository titleRepo;
    private final PositionRepository positionRepo;
    private final DocumentTypeRepository docTypeRepo;
    private final DocumentStatusRepository docStatusRepo;
    private final DocumentRepository documentRepo;
    private final UserProfileRepository userProfileRepo;
    private final ProcessRepository processRepo;
    private final ProcessIORepository ioRepo;
    private final ProcessPartyRepository partyRepo;
    private final ProcessStepRepository stepRepo;
    private final ProcessLevelPatternRepository levelPatternRepo;
    private final ProcessCodeConfigRepository codeConfigRepo;
    private final ObjectMapper objectMapper;

    // ─── Birimler ───────────────────────────────────────────────
    @PostMapping("/units")
    public ResponseEntity<?> importUnits(@RequestParam("file") MultipartFile file) throws IOException {
        List<Object> saved = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    String name = cellStr(row.getCell(0));
                    String code = cellStr(row.getCell(1));
                    if (name.isBlank() || code.isBlank()) continue;
                    if (unitRepo.existsByCode(code)) {
                        warnings.add("Satır " + (i + 1) + ": '" + code + "' kodu zaten sistemde mevcut, atlandı");
                        continue;
                    }
                    OrgUnit unit = new OrgUnit();
                    unit.setName(name);
                    unit.setCode(code);
                    unit.setActive(true);
                    saved.add(unitRepo.save(unit));
                } catch (Exception e) {
                    errors.add("Satır " + (i + 1) + ": " + e.getMessage());
                }
            }
        }
        return ResponseEntity.ok(Map.of("imported", saved.size(), "warnings", warnings, "errors", errors));
    }

    // ─── Roller ─────────────────────────────────────────────────
    @PostMapping("/roles")
    public ResponseEntity<?> importRoles(@RequestParam("file") MultipartFile file) throws IOException {
        List<Object> saved = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    String name = cellStr(row.getCell(0));
                    String code = cellStr(row.getCell(1));
                    String type = cellStr(row.getCell(2));
                    if (name.isBlank() || code.isBlank()) continue;
                    if (roleRepo.existsByCode(code)) {
                        warnings.add("Satır " + (i + 1) + ": '" + code + "' kodu zaten sistemde mevcut, atlandı");
                        continue;
                    }
                    Role role = new Role();
                    role.setName(name);
                    role.setCode(code);
                    role.setDescription(cellStr(row.getCell(3)));
                    role.setActive(true);
                    try { role.setRoleType(Role.RoleType.valueOf(type.toUpperCase())); }
                    catch (Exception e) { role.setRoleType(Role.RoleType.OPERATIONAL); }
                    saved.add(roleRepo.save(role));
                } catch (Exception e) {
                    errors.add("Satır " + (i + 1) + ": " + e.getMessage());
                }
            }
        }
        return ResponseEntity.ok(Map.of("imported", saved.size(), "warnings", warnings, "errors", errors));
    }

    // ─── Unvanlar ───────────────────────────────────────────────
    @PostMapping("/titles")
    public ResponseEntity<?> importTitles(@RequestParam("file") MultipartFile file) throws IOException {
        List<Object> saved = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    String name = cellStr(row.getCell(0));
                    String code = cellStr(row.getCell(1));
                    if (name.isBlank() || code.isBlank()) continue;
                    if (titleRepo.existsByCode(code)) {
                        warnings.add("Satır " + (i + 1) + ": '" + code + "' kodu zaten sistemde mevcut, atlandı");
                        continue;
                    }
                    Title t = new Title();
                    t.setName(name);
                    t.setCode(code);
                    t.setActive(true);
                    saved.add(titleRepo.save(t));
                } catch (Exception e) {
                    errors.add("Satır " + (i + 1) + ": " + e.getMessage());
                }
            }
        }
        return ResponseEntity.ok(Map.of("imported", saved.size(), "warnings", warnings, "errors", errors));
    }

    // ─── Pozisyonlar ────────────────────────────────────────────
    @PostMapping("/positions")
    public ResponseEntity<?> importPositions(@RequestParam("file") MultipartFile file) throws IOException {
        List<Object> saved = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    String name = cellStr(row.getCell(0));
                    String code = cellStr(row.getCell(1));
                    String level = cellStr(row.getCell(2));
                    if (name.isBlank() || code.isBlank()) continue;
                    if (positionRepo.existsByCode(code)) {
                        warnings.add("Satır " + (i + 1) + ": '" + code + "' kodu zaten sistemde mevcut, atlandı");
                        continue;
                    }
                    Position p = new Position();
                    p.setName(name);
                    p.setCode(code);
                    p.setDescription(cellStr(row.getCell(3)));
                    p.setActive(true);
                    try { p.setLevel(Position.Level.valueOf(level.toUpperCase())); }
                    catch (Exception e) { p.setLevel(Position.Level.STAFF); }
                    saved.add(positionRepo.save(p));
                } catch (Exception e) {
                    errors.add("Satır " + (i + 1) + ": " + e.getMessage());
                }
            }
        }
        return ResponseEntity.ok(Map.of("imported", saved.size(), "warnings", warnings, "errors", errors));
    }

    // ─── Doküman Türleri ────────────────────────────────────────
    @PostMapping("/doc-types")
    public ResponseEntity<?> importDocTypes(@RequestParam("file") MultipartFile file) throws IOException {
        List<Object> saved = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    String code = cellStr(row.getCell(0));
                    String name = cellStr(row.getCell(1));
                    if (code.isBlank() || name.isBlank()) continue;
                    if (docTypeRepo.findByCode(code).isPresent()) {
                        warnings.add("Satır " + (i + 1) + ": '" + code + "' kodu zaten sistemde mevcut, atlandı");
                        continue;
                    }
                    DocumentType t = new DocumentType();
                    t.setCode(code);
                    t.setName(name);
                    t.setLevel(cellInt(row.getCell(2), 1));
                    t.setDescription(cellStr(row.getCell(3)));
                    t.setActive(true);
                    saved.add(docTypeRepo.save(t));
                } catch (Exception e) {
                    errors.add("Satır " + (i + 1) + ": " + e.getMessage());
                }
            }
        }
        return ResponseEntity.ok(Map.of("imported", saved.size(), "warnings", warnings, "errors", errors));
    }

    // ─── Doküman Durumları ──────────────────────────────────────
    @PostMapping("/doc-statuses")
    public ResponseEntity<?> importDocStatuses(@RequestParam("file") MultipartFile file) throws IOException {
        List<Object> saved = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    String code = cellStr(row.getCell(0));
                    String name = cellStr(row.getCell(1));
                    if (code.isBlank() || name.isBlank()) continue;
                    if (docStatusRepo.findByCode(code).isPresent()) {
                        warnings.add("Satır " + (i + 1) + ": '" + code + "' kodu zaten sistemde mevcut, atlandı");
                        continue;
                    }
                    DocumentStatus s = new DocumentStatus();
                    s.setCode(code);
                    s.setName(name);
                    s.setOrder(cellInt(row.getCell(2), i));
                    s.setActive(true);
                    saved.add(docStatusRepo.save(s));
                } catch (Exception e) {
                    errors.add("Satır " + (i + 1) + ": " + e.getMessage());
                }
            }
        }
        return ResponseEntity.ok(Map.of("imported", saved.size(), "warnings", warnings, "errors", errors));
    }

    // ─── Dokümanlar ─────────────────────────────────────────────
    @PostMapping("/documents")
    public ResponseEntity<?> importDocuments(@RequestParam("file") MultipartFile file) throws IOException {
        List<Object> saved = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    String code  = cellStr(row.getCell(0));
                    String title = cellStr(row.getCell(1));
                    if (code.isBlank() || title.isBlank()) continue;

                    boolean isUpdate = documentRepo.findByCode(code).isPresent();
                    if (isUpdate) {
                        warnings.add("Satır " + (i + 1) + ": '" + code + "' kodu zaten sistemde mevcut, güncellendi");
                    }

                    Document doc = documentRepo.findByCode(code).orElse(new Document());
                    doc.setCode(code);
                    doc.setTitle(title);

                    String typeCode = cellStr(row.getCell(2));
                    if (!typeCode.isBlank())
                        docTypeRepo.findByCode(typeCode).ifPresent(doc::setDocType);

                    String statusCode = cellStr(row.getCell(3));
                    if (!statusCode.isBlank())
                        docStatusRepo.findByCode(statusCode).ifPresent(doc::setStatus);

                    String parentCode = cellStr(row.getCell(4));
                    if (!parentCode.isBlank())
                        documentRepo.findByCode(parentCode).ifPresent(doc::setParent);

                    saved.add(documentRepo.save(doc));
                } catch (Exception e) {
                    errors.add("Satır " + (i + 1) + ": " + e.getMessage());
                }
            }
        }
        return ResponseEntity.ok(Map.of("imported", saved.size(), "warnings", warnings, "errors", errors));
    }

    // ─── Kullanıcılar ───────────────────────────────────────────
    @PostMapping("/users")
    public ResponseEntity<?> importUsers(@RequestParam("file") MultipartFile file) throws IOException {
        List<Object> saved = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    String firstName   = cellStr(row.getCell(0));
                    String lastName    = cellStr(row.getCell(1));
                    String employeeId  = cellStr(row.getCell(2));
                    String phone       = cellStr(row.getCell(3));
                    String email       = cellStr(row.getCell(4));
                    String hireDate    = cellStr(row.getCell(5));
                    String unitCode    = cellStr(row.getCell(6));
                    String posCode     = cellStr(row.getCell(7));
                    String titleCode   = cellStr(row.getCell(8));
                    if (firstName.isBlank() && lastName.isBlank()) continue;

                    UserProfile profile;
                    boolean isUpdate = !employeeId.isBlank() && userProfileRepo.existsByEmployeeId(employeeId);
                    if (isUpdate) {
                        profile = userProfileRepo.findByEmployeeId(employeeId).get();
                        warnings.add("Satır " + (i + 1) + ": '" + employeeId + "' sicil nolu kayıt güncellendi");
                    } else {
                        profile = new UserProfile();
                    }
                    profile.setFirstName(firstName);
                    profile.setLastName(lastName);
                    profile.setFullName((firstName + " " + lastName).trim());
                    if (!employeeId.isBlank()) profile.setEmployeeId(employeeId);
                    if (!phone.isBlank())      profile.setPhone(phone);
                    if (!email.isBlank())      profile.setEmail(email);
                    if (!hireDate.isBlank()) {
                        try { profile.setHireDate(java.time.LocalDate.parse(hireDate)); }
                        catch (Exception ignored) {}
                    }
                    profile.setActive(true);
                    if (!titleCode.isBlank()) titleRepo.findByCode(titleCode).ifPresent(profile::setTitle);
                    if (!unitCode.isBlank())  unitRepo.findByCode(unitCode).ifPresent(profile::setOrgUnit);
                    if (!posCode.isBlank())   positionRepo.findByCode(posCode).ifPresent(profile::setPosition);
                    saved.add(userProfileRepo.save(profile));
                } catch (Exception e) {
                    errors.add("Satır " + (i + 1) + ": " + e.getMessage());
                }
            }
        }
        return ResponseEntity.ok(Map.of("imported", saved.size(), "warnings", warnings, "errors", errors));
    }

    // ─── Süreçler ───────────────────────────────────────────────
    // Sütunlar: Kod | Ad | Seviye (opsiyonel) | Tür | Kritiklik | Üst Süreç Kodu (opsiyonel) | Rol Kodu | Kısa Açıklama
    // Seviye ve Üst Süreç Kodu boş bırakılırsa kod yapısından (en uzun önek eşleşmesi) otomatik belirlenir.
    // Separator nokta (.) veya tire (-) olabilir — her ikisi de desteklenir.
    @PostMapping("/processes")
    public ResponseEntity<?> importProcesses(@RequestParam("file") MultipartFile file) throws IOException {
        List<String> warnings = new ArrayList<>();
        List<String> errors   = new ArrayList<>();

        // Mevcut kodları önceden yükle
        Set<String> usedCodes = new HashSet<>(
            processRepo.findAll().stream().map(Process::getCode).toList()
        );
        // Rol haritası
        Map<String, com.kys.organization.entity.Role> roleByCode = new HashMap<>();
        roleRepo.findAll().forEach(r -> roleByCode.put(r.getCode(), r));
        // Kod yapısı segment → seviye haritası (DB'den — process_level_patterns tablosu)
        Map<String, Process.Level> levelPatterns = loadLevelPatterns();
        // Yeni sistem: ProcessCodeConfig (varsa önce bunu kullan)
        boolean useNewConfig = false;
        String cfgSep = "-";
        List<Map<String, Object>> cfgLevelRules = List.of();
        try {
            ProcessCodeConfig codeConfig = codeConfigRepo.findFirstByOrderByIdAsc().orElse(null);
            if (codeConfig != null && codeConfig.getFieldNamesJson() != null && codeConfig.getLevelRulesJson() != null) {
                List<String> cfgFieldNames = objectMapper.readValue(codeConfig.getFieldNamesJson(), new TypeReference<>() {});
                cfgLevelRules = objectMapper.readValue(codeConfig.getLevelRulesJson(), new TypeReference<>() {});
                cfgSep = codeConfig.getSeparator() != null ? codeConfig.getSeparator() : "-";
                useNewConfig = !cfgFieldNames.isEmpty() && !cfgLevelRules.isEmpty();
            }
        } catch (Exception ignored) {}
        final boolean useNewCfg = useNewConfig;
        final String newCfgSep = cfgSep;
        final List<Map<String, Object>> newCfgRules = cfgLevelRules;

        // ── Geçiş 1: tüm süreçleri kaydet (parent olmadan) ──────
        Map<String, Process> importedByCode = new HashMap<>(); // bu import'ta eklenenler
        Set<String> explicitParentSet = new HashSet<>();       // explicit parent kodu verilen kodlar
        List<String[]> parentCodePairs = new ArrayList<>();    // [finalCode, parentCode]

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    String code = cellStr(row.getCell(0));
                    String name = cellStr(row.getCell(1));
                    if (code.isBlank() || name.isBlank()) continue;

                    // Duplicate kod → otomatik suffix
                    String finalCode = code;
                    if (usedCodes.contains(code)) {
                        int n = 2;
                        while (usedCodes.contains(code + "_" + n)) n++;
                        finalCode = code + "_" + n;
                        warnings.add("Satır " + (i+1) + ": '" + code + "' kodu kullanımda → '" + finalCode + "' olarak eklendi");
                    }
                    usedCodes.add(finalCode);

                    Process proc = new Process();
                    proc.setCode(finalCode);
                    proc.setName(name);

                    // Seviye: kod yapısından tespit et + sütunla karşılaştır
                    String levelStr = cellStr(row.getCell(2));
                    Process.Level detectedLevel = useNewCfg
                            ? detectLevelFromCodeNew(finalCode, newCfgSep, newCfgRules)
                            : detectLevelFromCode(finalCode, levelPatterns);
                    Process.Level columnLevel    = parseLevel(levelStr);

                    if (columnLevel != null && columnLevel != detectedLevel) {
                        // Sütun değeri var ama kod yapısıyla uyuşmuyor → uyarı ver, sütunu kullan
                        warnings.add("Satır " + (i+1) + " [" + finalCode + "]: sütunda "
                            + columnLevel + " yazıyor, kod yapısından " + detectedLevel
                            + " tespit edildi. Sütundaki değer kullanıldı — kodu kontrol edin.");
                        proc.setLevel(columnLevel);
                    } else if (columnLevel != null) {
                        proc.setLevel(columnLevel);   // sütun doğru, kullan
                    } else {
                        proc.setLevel(detectedLevel); // sütun boş/geçersiz, koddan tespit edileni kullan
                    }

                    String typeStr = cellStr(row.getCell(3));
                    if (!typeStr.isBlank()) {
                        try { proc.setProcessType(Process.ProcessType.valueOf(typeStr.toUpperCase())); }
                        catch (Exception ignored) {}
                    }
                    String critStr = cellStr(row.getCell(4));
                    if (!critStr.isBlank()) {
                        try { proc.setCriticality(Process.Criticality.valueOf(critStr.toUpperCase())); }
                        catch (Exception ignored) {}
                    }

                    // Üst süreç kodu: explicit verildiyse 2. geçişte işle
                    String parentCode = cellStr(row.getCell(5));
                    if (!parentCode.isBlank()) {
                        parentCodePairs.add(new String[]{finalCode, parentCode});
                        explicitParentSet.add(finalCode);
                    }
                    // Boş bırakılanlar geçiş 3'te en uzun önek eşleşmesiyle bulunacak

                    String roleCode = cellStr(row.getCell(6));
                    if (!roleCode.isBlank() && roleByCode.containsKey(roleCode)) {
                        proc.setOwnerRole(roleByCode.get(roleCode));
                    }
                    String desc = cellStr(row.getCell(7));
                    if (!desc.isBlank()) proc.setShortDescription(desc);

                    proc.setActive(true);
                    if (proc.getStatus() == null) proc.setStatus(Process.Status.ACTIVE);
                    processRepo.save(proc);
                    importedByCode.put(finalCode, proc);
                } catch (Exception e) {
                    errors.add("Satır " + (i+1) + ": " + e.getMessage());
                }
            }
        }

        // ── Geçiş 2: explicit üst süreç bağlantılarını kur ──────
        Map<String, Process> allByCode = new HashMap<>();
        processRepo.findAll().forEach(p -> allByCode.put(p.getCode(), p));

        int linked = 0;
        for (String[] pair : parentCodePairs) {
            String childCode  = pair[0];
            String parentCode = pair[1];
            Process child  = allByCode.get(childCode);
            Process parent = allByCode.get(parentCode);
            if (child != null && parent != null) {
                child.setParent(parent);
                processRepo.save(child);
                linked++;
            } else if (child != null) {
                warnings.add("'" + childCode + "' için üst kod bulunamadı: '" + parentCode + "'");
            }
        }

        // ── Geçiş 3: parent sütunu boş olanlar için en uzun önek eşleşmesi ──
        // Hem nokta (.) hem tire (-) separator desteklenir.
        Collection<String> allCodes = allByCode.keySet();
        for (Map.Entry<String, Process> entry : importedByCode.entrySet()) {
            String code = entry.getKey();
            Process proc = entry.getValue();
            if (explicitParentSet.contains(code)) continue; // explicit parent verilmişse atla
            if (proc.getParent() != null) continue;

            Process bestParent = null;
            int bestLen = 0;
            for (String cCode : allCodes) {
                if (cCode.equals(code)) continue;
                if ((code.startsWith(cCode + ".") || code.startsWith(cCode + "-"))
                        && cCode.length() > bestLen) {
                    bestParent = allByCode.get(cCode);
                    bestLen = cCode.length();
                }
            }
            if (bestParent != null) {
                proc.setParent(bestParent);
                processRepo.save(proc);
                linked++;
            }
        }

        // ── Geçiş 4: L2 → L1 bağlantısı (segment değişimli kodlar için) ─────
        // Örn: KADEM-VKF-SR-01 → SR=L2 segmenti, öncesi KADEM-VKF,
        //      aynı prefix + L1 segmenti (AS) ile başlayan → KADEM-VKF-AS-01 parent olur.
        // Bu geçiş, prefix eşleşmesinin çalışmadığı L2→L1 durumunu kapatır.
        if (!levelPatterns.isEmpty()) {
            // Ters harita: seviye → segment (L1→"AS", L2→"SR")
            Map<Process.Level, String> levelToSeg = new HashMap<>();
            levelPatterns.forEach((seg, lvl) -> levelToSeg.put(lvl, seg));
            String l1Seg = levelToSeg.get(Process.Level.L1);

            if (l1Seg != null) {
                for (Map.Entry<String, Process> entry : importedByCode.entrySet()) {
                    Process proc = entry.getValue();
                    if (proc.getParent() != null) continue;
                    if (proc.getLevel() != Process.Level.L2) continue;

                    String code   = entry.getKey();
                    String[] parts = code.split("[-.]");

                    // Koddaki L2 segmentinin konumunu bul
                    int segIdx = -1;
                    for (int i = 0; i < parts.length; i++) {
                        Process.Level lv = levelPatterns.get(parts[i].toUpperCase());
                        if (lv == Process.Level.L2) { segIdx = i; break; }
                    }
                    if (segIdx < 0) continue;

                    // Segment öncesi prefix: "KADEM-VKF"
                    String unitPrefix  = String.join("-", Arrays.copyOfRange(parts, 0, segIdx));
                    String searchStart = unitPrefix + "-" + l1Seg + "-";

                    // En kısa eşleşen L1'i bul (doğrudan parent)
                    Process parentL1  = null;
                    int minLen = Integer.MAX_VALUE;
                    for (Map.Entry<String, Process> c : allByCode.entrySet()) {
                        String cCode = c.getKey();
                        if (cCode.startsWith(searchStart) && cCode.length() < minLen) {
                            parentL1 = c.getValue();
                            minLen   = cCode.length();
                        }
                    }
                    if (parentL1 != null) {
                        proc.setParent(parentL1);
                        processRepo.save(proc);
                        linked++;
                    }
                }
            }
        }

        // ── Geçiş 5: L2→L1 ortak token önek eşleşmesi (fallback) ───
        // Örn: KADEM-VKF-SR-01 → KADEM-VKF-AS-01 (aynı Org-Birim prefix, farklı tür segmenti)
        // Geçiş 3 ve 4 başarısız olduğunda çalışır.
        final String finalSepForFallback = cfgSep;
        for (Map.Entry<String, Process> entry : importedByCode.entrySet()) {
            Process proc = entry.getValue();
            if (proc.getParent() != null) continue;
            if (proc.getLevel() != Process.Level.L2) continue;
            String code = entry.getKey();
            String[] tokens = code.split(java.util.regex.Pattern.quote(finalSepForFallback));
            if (tokens.length < 2) continue;
            String orgBirimPrefix = tokens[0] + finalSepForFallback + tokens[1] + finalSepForFallback;
            Process candidate = null;
            int minLen = Integer.MAX_VALUE;
            for (Map.Entry<String, Process> c : allByCode.entrySet()) {
                String cCode = c.getKey();
                if (cCode.equals(code)) continue;
                if (c.getValue().getLevel() != Process.Level.L1) continue;
                if (cCode.startsWith(orgBirimPrefix) && cCode.length() < minLen) {
                    candidate = c.getValue();
                    minLen = cCode.length();
                }
            }
            if (candidate != null) {
                proc.setParent(candidate);
                processRepo.save(proc);
                linked++;
            }
        }

        // NOT: Seviyeler Excel sütunundan okunur, hiyerarşiden hesaplanmaz.
        // Seviye sütunu boş bırakılırsa L1 olarak kaydedilir.
        // İlişkiler kurulduktan sonra seviyeler otomatik düzeltilmez — bu bilerek yapılan tasarım tercihidir.

        return ResponseEntity.ok(Map.of(
            "imported", importedByCode.size(),
            "linked",   linked,
            "warnings", warnings,
            "errors",   errors
        ));
    }

    // ─── Süreç Detayları (IO / Parti / Adım) ────────────────────
    @PostMapping("/process-details")
    public ResponseEntity<?> importProcessDetails(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mode", defaultValue = "append") String mode) throws IOException {

        boolean replace = "replace".equalsIgnoreCase(mode);

        // Süreç kodu → Process haritası
        Map<String, Process> byCode = new HashMap<>();
        processRepo.findAll().forEach(p -> byCode.put(p.getCode(), p));

        // Etkilenen süreç kodları (replace modunda silinecekler için)
        Set<String> affectedCodes = new HashSet<>();

        // Sonuç sayaçları ve hata listeleri
        int ioAdded = 0, partyAdded = 0, stepAdded = 0;
        List<String> ioErrors = new ArrayList<>();
        List<String> partyErrors = new ArrayList<>();
        List<String> stepErrors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {

            // ── replace modunda: etkilenen kodları topla, mevcut kayıtları sil ──
            if (replace) {
                for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                    Sheet sheet = wb.getSheetAt(s);
                    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row == null) continue;
                        String code = cellStr(row.getCell(0));
                        if (!code.isBlank() && byCode.containsKey(code)) {
                            affectedCodes.add(code);
                        }
                    }
                }
                for (String code : affectedCodes) {
                    Process proc = byCode.get(code);
                    ioRepo.deleteAll(ioRepo.findByProcessId(proc.getId()));
                    partyRepo.deleteAll(partyRepo.findByProcessId(proc.getId()));
                    stepRepo.deleteAll(stepRepo.findByProcessIdOrderByStepNoAsc(proc.getId()));
                }
            }

            // ── IO sheet ────────────────────────────────────────────────────
            Sheet ioSheet = findSheet(wb, "io", "girdi", "girdi-çıktı", "girdi-cikti");
            if (ioSheet != null) {
                for (int i = 1; i <= ioSheet.getLastRowNum(); i++) {
                    Row row = ioSheet.getRow(i);
                    if (row == null) continue;
                    try {
                        String code = cellStr(row.getCell(0));
                        String type = cellStr(row.getCell(1)).toUpperCase(java.util.Locale.ROOT);
                        String name = cellStr(row.getCell(2));
                        if (code.isBlank() || name.isBlank()) continue;
                        Process proc = byCode.get(code);
                        if (proc == null) {
                            ioErrors.add("Satır " + (i + 1) + ": '" + code + "' kodu bulunamadı");
                            continue;
                        }
                        ProcessIO.IOType ioType = parseIOType(type);
                        if (ioType == null) {
                            ioErrors.add("Satır " + (i + 1) + ": geçersiz tür '" + type + "'");
                            continue;
                        }
                        ProcessIO io = new ProcessIO();
                        io.setProcess(proc);
                        io.setName(name);
                        io.setIoType(ioType);
                        ioRepo.save(io);
                        ioAdded++;
                    } catch (Exception e) {
                        ioErrors.add("Satır " + (i + 1) + ": " + e.getMessage());
                    }
                }
            } else {
                warnings.add("IO/Girdi-Çıktı sayfası bulunamadı");
            }

            // ── Parti sheet ─────────────────────────────────────────────────
            Sheet partySheet = findSheet(wb, "parti", "parties", "tedarikçi", "tedarikci",
                    "tedarikçi-müşteri", "tedarikci-musteri");
            if (partySheet != null) {
                for (int i = 1; i <= partySheet.getLastRowNum(); i++) {
                    Row row = partySheet.getRow(i);
                    if (row == null) continue;
                    try {
                        String code = cellStr(row.getCell(0));
                        String type = cellStr(row.getCell(1)).toUpperCase(java.util.Locale.ROOT);
                        String name = cellStr(row.getCell(2));
                        if (code.isBlank() || name.isBlank()) continue;
                        Process proc = byCode.get(code);
                        if (proc == null) {
                            partyErrors.add("Satır " + (i + 1) + ": '" + code + "' kodu bulunamadı");
                            continue;
                        }
                        ProcessParty.PartyType partyType = parsePartyType(type);
                        if (partyType == null) {
                            partyErrors.add("Satır " + (i + 1) + ": geçersiz tür '" + type + "'");
                            continue;
                        }
                        ProcessParty party = new ProcessParty();
                        party.setProcess(proc);
                        party.setName(name);
                        party.setPartyType(partyType);
                        partyRepo.save(party);
                        partyAdded++;
                    } catch (Exception e) {
                        partyErrors.add("Satır " + (i + 1) + ": " + e.getMessage());
                    }
                }
            } else {
                warnings.add("Tedarikçi-Müşteri/Parties sayfası bulunamadı");
            }

            // ── Adım sheet ──────────────────────────────────────────────────
            Sheet stepSheet = findSheet(wb, "adım", "adim", "steps");
            if (stepSheet != null) {
                for (int i = 1; i <= stepSheet.getLastRowNum(); i++) {
                    Row row = stepSheet.getRow(i);
                    if (row == null) continue;
                    try {
                        String code = cellStr(row.getCell(0));
                        String stepNoStr = cellStr(row.getCell(1));
                        String name = cellStr(row.getCell(2));
                        String desc = cellStr(row.getCell(3));
                        if (code.isBlank() || name.isBlank()) continue;
                        Process proc = byCode.get(code);
                        if (proc == null) {
                            stepErrors.add("Satır " + (i + 1) + ": '" + code + "' kodu bulunamadı");
                            continue;
                        }
                        ProcessStep step = new ProcessStep();
                        step.setProcess(proc);
                        step.setName(name);
                        step.setDescription(desc.isBlank() ? null : desc);
                        try { step.setStepNo(Integer.parseInt(stepNoStr)); }
                        catch (Exception ignored) { step.setStepNo(i); }
                        stepRepo.save(step);
                        stepAdded++;
                    } catch (Exception e) {
                        stepErrors.add("Satır " + (i + 1) + ": " + e.getMessage());
                    }
                }
            } else {
                warnings.add("Adım/Steps sayfası bulunamadı");
            }
        }

        return ResponseEntity.ok(Map.of(
            "io",       Map.of("added", ioAdded,    "errors", ioErrors),
            "parties",  Map.of("added", partyAdded, "errors", partyErrors),
            "steps",    Map.of("added", stepAdded,  "errors", stepErrors),
            "warnings", warnings
        ));
    }

    @GetMapping("/process-details/template")
    public ResponseEntity<byte[]> processDetailsTemplate() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ── IO sheet ────────────────────────────────────────────────────
            Sheet ioSheet = wb.createSheet("IO");
            Row ioHeader = ioSheet.createRow(0);
            ioHeader.createCell(0).setCellValue("Süreç Kodu");
            ioHeader.createCell(1).setCellValue("Tür");
            ioHeader.createCell(2).setCellValue("Ad");
            Row ioEx1 = ioSheet.createRow(1);
            ioEx1.createCell(0).setCellValue("KADEM-SY-01");
            ioEx1.createCell(1).setCellValue("GİRDİ");
            ioEx1.createCell(2).setCellValue("Müşteri Talebi");
            Row ioEx2 = ioSheet.createRow(2);
            ioEx2.createCell(0).setCellValue("KADEM-SY-01");
            ioEx2.createCell(1).setCellValue("ÇIKTI");
            ioEx2.createCell(2).setCellValue("Onaylı Sipariş");

            // ── Tedarikçi-Müşteri sheet ─────────────────────────────────────
            Sheet partySheet = wb.createSheet("Tedarikçi-Müşteri");
            Row partyHeader = partySheet.createRow(0);
            partyHeader.createCell(0).setCellValue("Süreç Kodu");
            partyHeader.createCell(1).setCellValue("Tür");
            partyHeader.createCell(2).setCellValue("Ad");
            Row partyEx1 = partySheet.createRow(1);
            partyEx1.createCell(0).setCellValue("KADEM-SY-01");
            partyEx1.createCell(1).setCellValue("TEDARİKÇİ");
            partyEx1.createCell(2).setCellValue("Satın Alma");
            Row partyEx2 = partySheet.createRow(2);
            partyEx2.createCell(0).setCellValue("KADEM-SY-01");
            partyEx2.createCell(1).setCellValue("MÜŞTERİ");
            partyEx2.createCell(2).setCellValue("Kalite Birimi");

            // ── Adımlar sheet ───────────────────────────────────────────────
            Sheet stepSheet = wb.createSheet("Adımlar");
            Row stepHeader = stepSheet.createRow(0);
            stepHeader.createCell(0).setCellValue("Süreç Kodu");
            stepHeader.createCell(1).setCellValue("Sıra No");
            stepHeader.createCell(2).setCellValue("Ad");
            stepHeader.createCell(3).setCellValue("Açıklama");
            Row stepEx = stepSheet.createRow(1);
            stepEx.createCell(0).setCellValue("KADEM-SY-01");
            stepEx.createCell(1).setCellValue(1);
            stepEx.createCell(2).setCellValue("Talep Al");
            stepEx.createCell(3).setCellValue("Müşteriden talep alınır");

            wb.write(out);
            byte[] bytes = out.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "surec-detaylari-sablon.xlsx");
            headers.setContentLength(bytes.length);
            return ResponseEntity.ok().headers(headers).body(bytes);
        }
    }

    // ─── Yardımcı metodlar ──────────────────────────────────────

    /**
     * Excel seviye sütununu okur. Desteklenen formatlar:
     *   L1, L2, L3 | 1, 2, 3 | Seviye 1 / Seviye 2 | LEVEL_1 vb.
     */
    private Process.Level parseLevel(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim().toUpperCase().replaceAll("[^A-Z0-9]", ""); // yalnızca harf+rakam
        // Tam eşleşme
        if (s.equals("L1") || s.equals("1") || s.endsWith("1")) return Process.Level.L1;
        if (s.equals("L2") || s.equals("2") || s.endsWith("2")) return Process.Level.L2;
        if (s.equals("L3") || s.equals("3") || s.endsWith("3")) return Process.Level.L3;
        return null;
    }

    /**
     * Kod yapısından seviye tespit eder.
     *
     * Önce DB'deki segment→seviye tablosu kullanılır (levelPatterns).
     *   Örn. AS→L1, SR→L2 tanımlıysa:
     *     KADEM-VKF-AS-01      → AS bulundu(L1) + 1 sayısal  → L1
     *     KADEM-VKF-SR-01      → SR bulundu(L2) + 1 sayısal  → L2
     *     KADEM-VKF-SR-01-001  → SR bulundu(L2) + 2 sayısal  → L3
     *
     * levelPatterns boşsa (hiç kayıt yoksa) eski sayısal-segment yöntemi çalışır:
     *   1 sayısal segment → L1, 2 → L2, 3+ → L3
     */
    private Process.Level detectLevelFromCode(String code, Map<String, Process.Level> levelPatterns) {
        if (code == null || code.isBlank()) return Process.Level.L1;
        String[] parts = code.split("[-.]");

        if (!levelPatterns.isEmpty()) {
            // Eşleşen segmenti ve konumunu bul
            for (int i = 0; i < parts.length; i++) {
                Process.Level base = levelPatterns.get(parts[i].toUpperCase());
                if (base != null) {
                    // Eşleşen segmentten sonra gelen sayısal parça sayısı
                    int numericAfter = 0;
                    for (int j = i + 1; j < parts.length; j++) {
                        if (parts[j].matches("\\d+")) numericAfter++;
                    }
                    // Her ekstra sayısal için seviyeyi bir üste çık, L3'te sınırla
                    Process.Level[] levels = Process.Level.values();
                    int ordinal = Math.min(base.ordinal() + Math.max(0, numericAfter - 1), levels.length - 1);
                    return levels[ordinal];
                }
            }
        }

        // Fallback: sadece sayısal segment sayısına bak
        int numericCount = 0;
        for (String part : parts) {
            if (!part.isBlank() && part.matches("\\d+")) numericCount++;
        }
        if (numericCount <= 1) return Process.Level.L1;
        if (numericCount == 2) return Process.Level.L2;
        return Process.Level.L3;
    }

    private String cellStr(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private int cellInt(Cell cell, int defaultVal) {
        if (cell == null) return defaultVal;
        if (cell.getCellType() == CellType.NUMERIC) return (int) cell.getNumericCellValue();
        try { return Integer.parseInt(cellStr(cell)); }
        catch (Exception e) { return defaultVal; }
    }

    /**
     * Yeni sistem: ProcessCodeConfig kurallarıyla seviye tespit eder.
     * applyLevelRules'ı ProcessCodeConfigController'dan çağırır.
     */
    private Process.Level detectLevelFromCodeNew(String code, String sep, List<Map<String, Object>> rules) {
        if (code == null || code.isBlank()) return Process.Level.L1;
        String[] tokens = code.split(java.util.regex.Pattern.quote(sep));
        String level = ProcessCodeConfigController.applyLevelRules(tokens, rules);
        if (level != null) {
            try { return Process.Level.valueOf(level); } catch (Exception ignored) {}
        }
        return Process.Level.L1;
    }

    /** process_level_patterns tablosunu segment→seviye haritasına dönüştürür. */
    private Map<String, Process.Level> loadLevelPatterns() {
        Map<String, Process.Level> map = new HashMap<>();
        levelPatternRepo.findAll().forEach(p -> map.put(p.getSegment().toUpperCase(), p.getBaseLevel()));
        return map;
    }

    /** Sayfa adını esnek eşleşmeyle bulur (Türkçe/İngilizce, büyük/küçük harf). */
    private Sheet findSheet(Workbook wb, String... candidates) {
        for (int s = 0; s < wb.getNumberOfSheets(); s++) {
            String sheetName = wb.getSheetName(s).toLowerCase(java.util.Locale.ROOT).trim();
            for (String c : candidates) {
                if (sheetName.contains(c.toLowerCase(java.util.Locale.ROOT))) {
                    return wb.getSheetAt(s);
                }
            }
        }
        return null;
    }

    private ProcessIO.IOType parseIOType(String raw) {
        return switch (raw) {
            case "INPUT", "GİRDİ", "GIRDI" -> ProcessIO.IOType.INPUT;
            case "OUTPUT", "ÇIKTI", "CIKTI" -> ProcessIO.IOType.OUTPUT;
            default -> null;
        };
    }

    private ProcessParty.PartyType parsePartyType(String raw) {
        return switch (raw) {
            case "SUPPLIER", "TEDARİKÇİ", "TEDARIKCI" -> ProcessParty.PartyType.SUPPLIER;
            case "CUSTOMER", "MÜŞTERİ", "MUSTERI" -> ProcessParty.PartyType.CUSTOMER;
            default -> null;
        };
    }
}
