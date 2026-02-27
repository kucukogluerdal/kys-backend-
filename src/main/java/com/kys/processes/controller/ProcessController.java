package com.kys.processes.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kys.organization.repository.PositionRepository;
import com.kys.organization.repository.RoleRepository;
import com.kys.processes.entity.KPI;
import com.kys.processes.entity.Process;
import com.kys.processes.entity.ProcessCodeConfig;
import com.kys.processes.entity.ProcessIO;
import com.kys.processes.entity.ProcessParty;
import com.kys.processes.entity.ProcessRisk;
import com.kys.processes.entity.ProcessStakeholder;
import com.kys.processes.entity.ProcessStep;
import com.kys.processes.repository.KPIRepository;
import com.kys.processes.repository.ProcessCodeConfigRepository;
import com.kys.processes.repository.ProcessIORepository;
import com.kys.processes.repository.ProcessLevelPatternRepository;
import com.kys.processes.repository.ProcessPartyRepository;
import com.kys.processes.repository.ProcessRepository;
import com.kys.processes.repository.ProcessRiskRepository;
import com.kys.processes.repository.ProcessStakeholderRepository;
import com.kys.processes.repository.ProcessStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/processes")
@RequiredArgsConstructor
public class ProcessController {

    private final ProcessRepository repo;
    private final RoleRepository roleRepo;
    private final PositionRepository positionRepo;
    private final ProcessIORepository ioRepo;
    private final ProcessPartyRepository partyRepo;
    private final ProcessStepRepository stepRepo;
    private final ProcessRiskRepository riskRepo;
    private final KPIRepository kpiRepo;
    private final ProcessLevelPatternRepository levelPatternRepo;
    private final ProcessCodeConfigRepository codeConfigRepo;
    private final ProcessStakeholderRepository stakeholderRepo;
    private final ObjectMapper objectMapper;

    // ─── Otomatik ilişkilendirme ──────────────────────────────────

    @PostMapping("/auto-link")
    @Transactional
    public ResponseEntity<?> autoLink() {
        List<Process> all = repo.findAll();
        int linked = 0, leveled = 0, cleared = 0;
        List<String> warnings = new ArrayList<>();
        List<Map<String, Object>> changes = new ArrayList<>();

        // ── ProcessCodeConfig yükle (yeni sistem) ────────────────────────
        ProcessCodeConfig codeConfig = codeConfigRepo.findFirstByOrderByIdAsc().orElse(null);
        List<String> fieldNames = List.of();
        List<Map<String, Object>> levelRules = List.of();
        String cfgSep = "-";
        if (codeConfig != null) {
            try {
                cfgSep = codeConfig.getSeparator() != null ? codeConfig.getSeparator() : "-";
                if (codeConfig.getFieldNamesJson() != null)
                    fieldNames = objectMapper.readValue(codeConfig.getFieldNamesJson(), new TypeReference<>() {});
                if (codeConfig.getLevelRulesJson() != null)
                    levelRules = objectMapper.readValue(codeConfig.getLevelRulesJson(), new TypeReference<>() {});
            } catch (Exception e) {
                warnings.add("Config parse hatası: " + e.getMessage());
                codeConfig = null;
            }
        }
        boolean useNewConfig = codeConfig != null && !fieldNames.isEmpty() && !levelRules.isEmpty();

        // Eski sistem: segment → seviye haritası (yedek)
        Map<String, Process.Level> levelPatterns = new HashMap<>();
        levelPatternRepo.findAll().forEach(lp ->
            levelPatterns.put(lp.getSegment().toUpperCase(), lp.getBaseLevel()));
        Map<Process.Level, String> levelToSeg = new HashMap<>();
        levelPatterns.forEach((seg, lv) -> levelToSeg.putIfAbsent(lv, seg));
        String l1Seg = levelToSeg.get(Process.Level.L1);

        // Kod → süreç index (yeni sistem için parent lookup)
        Map<String, Process> codeIndex = new HashMap<>();
        all.forEach(p -> { if (p.getCode() != null) codeIndex.put(p.getCode().trim(), p); });

        final String sepFinal = cfgSep;
        final List<String> fieldNamesFinal = fieldNames;
        final List<Map<String, Object>> levelRulesFinal = levelRules;

        for (Process p : all) {
            String code = p.getCode() == null ? "" : p.getCode().trim();
            if (code.isEmpty()) { warnings.add("Kodu boş süreç: id=" + p.getId()); continue; }

            // ── 1. Level tespiti ──────────────────────────────────────────
            if (useNewConfig) {
                // Yeni sistem: LevelRules
                String[] tokens = code.split(Pattern.quote(sepFinal));
                String detectedLevel = ProcessCodeConfigController.applyLevelRules(tokens, levelRulesFinal);
                if (detectedLevel != null) {
                    Process.Level newLevel = Process.Level.valueOf(detectedLevel);
                    if (p.getLevel() != newLevel) {
                        Map<String, Object> ch = new HashMap<>();
                        ch.put("code", code); ch.put("name", p.getName());
                        ch.put("action", "leveled");
                        ch.put("oldLevel", p.getLevel() != null ? p.getLevel().name() : null);
                        ch.put("newLevel", newLevel.name());
                        changes.add(ch);
                        p.setLevel(newLevel);
                        leveled++;
                    }
                }
            } else if (!levelPatterns.isEmpty()) {
                // Eski sistem: segment bazlı
                Process.Level newLevel = detectLevelFromCode(code, levelPatterns);
                if (p.getLevel() != newLevel) {
                    Map<String, Object> ch = new HashMap<>();
                    ch.put("code", code); ch.put("name", p.getName());
                    ch.put("action", "leveled");
                    ch.put("oldLevel", p.getLevel() != null ? p.getLevel().name() : null);
                    ch.put("newLevel", newLevel.name());
                    changes.add(ch);
                    p.setLevel(newLevel);
                    leveled++;
                }
            }

            // ── 2. Parent tespiti ──────────────────────────────────────────
            // L1 süreçler kök süreçtir: yanlış atanmış parent varsa temizle
            if (p.getLevel() == Process.Level.L1) {
                if (p.getParent() != null) {
                    Map<String, Object> ch = new HashMap<>();
                    ch.put("code", code); ch.put("name", p.getName());
                    ch.put("action", "cleared");
                    ch.put("oldParent", p.getParent().getCode());
                    ch.put("newParent", null);
                    changes.add(ch);
                    p.setParent(null);
                    cleared++;
                }
                continue;
            }

            Process bestParent = null;

            if (useNewConfig) {
                // ── YENİ SİSTEM: Template tabanlı parent ──────────────────
                String[] tokens = code.split(Pattern.quote(sepFinal));
                // Alan haritası oluştur
                Map<String, String> fields = new LinkedHashMap<>();
                for (int i = 0; i < fieldNamesFinal.size() && i < tokens.length; i++) {
                    fields.put(fieldNamesFinal.get(i), tokens[i]);
                }
                // Seviyeye göre şablon seç
                String template = p.getLevel() == Process.Level.L2
                        ? codeConfig.getParentTemplateL2()
                        : codeConfig.getParentTemplateL3();
                if (template != null && !template.isBlank()) {
                    String parentCode = ProcessCodeConfigController.expandTemplate(template, fields);
                    // Şablon expand edilebildiyse ve kod kendisiyle aynı değilse ara
                    if (parentCode != null && !parentCode.isBlank()
                            && !parentCode.equals(code)
                            && !parentCode.contains("{")) {
                        bestParent = codeIndex.get(parentCode);
                    }
                }
                // ── FALLBACK: Şablon başarısız olduysa prefix eşleşmesi dene ──
                if (bestParent == null) {
                    int bestLen = 0;
                    for (Process candidate : all) {
                        if (candidate.getId().equals(p.getId())) continue;
                        String cCode = candidate.getCode() == null ? "" : candidate.getCode().trim();
                        if ((code.startsWith(cCode + "-") || code.startsWith(cCode + "."))
                                && cCode.length() > bestLen) {
                            bestParent = candidate;
                            bestLen = cCode.length();
                        }
                    }
                }
                // ── FALLBACK 2: L2→L1 için ortak token önek eşleşmesi ──────
                // Örn: KADEM-VKF-SR-01 → KADEM-VKF-AS-01 (aynı Org-Birim, farklı tür segmenti)
                if (bestParent == null && p.getLevel() == Process.Level.L2 && tokens.length >= 2) {
                    String orgBirimPrefix = tokens[0] + sepFinal + tokens[1] + sepFinal;
                    Process candidate = null;
                    int minLen = Integer.MAX_VALUE;
                    for (Process c : all) {
                        if (c.getId().equals(p.getId())) continue;
                        if (c.getLevel() != Process.Level.L1) continue;
                        String cCode = c.getCode() == null ? "" : c.getCode().trim();
                        if (cCode.startsWith(orgBirimPrefix) && cCode.length() < minLen) {
                            candidate = c;
                            minLen = cCode.length();
                        }
                    }
                    if (candidate != null) bestParent = candidate;
                }
                // Hâlâ bulunamadıysa uyarı ver
                if (bestParent == null && template != null && !template.isBlank()) {
                    String parentCode = ProcessCodeConfigController.expandTemplate(template, fields);
                    warnings.add("Parent bulunamadı: " + code + " → beklenen: " + parentCode);
                }
            } else {
                // ── ESKİ SİSTEM: Prefix + cross-segment (yedek) ───────────
                Process.Level expectedParentLevel =
                    p.getLevel() == Process.Level.L2 ? Process.Level.L1 :
                    p.getLevel() == Process.Level.L3 ? Process.Level.L2 : null;

                int bestLen = 0;
                for (Process candidate : all) {
                    if (candidate.getId().equals(p.getId())) continue;
                    String cCode = candidate.getCode() == null ? "" : candidate.getCode().trim();
                    if ((code.startsWith(cCode + "-") || code.startsWith(cCode + "."))
                            && cCode.length() > bestLen) {
                        if (expectedParentLevel == null || candidate.getLevel() == expectedParentLevel) {
                            bestParent = candidate;
                            bestLen = cCode.length();
                        }
                    }
                }
                if (bestParent == null && !levelPatterns.isEmpty() && l1Seg != null
                        && p.getLevel() == Process.Level.L2) {
                    String[] parts = code.split("[-.]");
                    for (int i = 0; i < parts.length; i++) {
                        if (levelPatterns.get(parts[i].toUpperCase()) == Process.Level.L2) {
                            String unitPrefix = String.join("-", Arrays.copyOfRange(parts, 0, i));
                            String searchStart = unitPrefix.isEmpty() ? l1Seg + "-" : unitPrefix + "-" + l1Seg + "-";
                            int minLen = Integer.MAX_VALUE;
                            for (Process c : all) {
                                String cCode = c.getCode() == null ? "" : c.getCode().trim();
                                if (cCode.startsWith(searchStart) && cCode.length() < minLen
                                        && c.getLevel() == Process.Level.L1) {
                                    bestParent = c;
                                    minLen = cCode.length();
                                }
                            }
                            break;
                        }
                    }
                }
            }

            // Hiçbiri bulunamadıysa mevcut parent'ı koru
            if (bestParent == null) bestParent = p.getParent();

            // Parent değiştiyse kaydet
            if (bestParent != null) {
                boolean parentChanged = p.getParent() == null
                    || !bestParent.getId().equals(p.getParent().getId());
                if (parentChanged) {
                    Map<String, Object> ch = new HashMap<>();
                    ch.put("code", code); ch.put("name", p.getName());
                    ch.put("action", p.getParent() == null ? "linked" : "relinked");
                    ch.put("oldParent", p.getParent() != null ? p.getParent().getCode() : null);
                    ch.put("newParent", bestParent.getCode());
                    changes.add(ch);
                    p.setParent(bestParent);
                    linked++;
                }
            }
        }

        repo.saveAll(all);

        Map<String, Object> result = new HashMap<>();
        result.put("total",    all.size());
        result.put("linked",   linked);
        result.put("leveled",  leveled);
        result.put("cleared",  cleared);
        result.put("changes",  changes);
        result.put("warnings", warnings);
        return ResponseEntity.ok(result);
    }

    /** Import ile aynı level tespit mantığı — pattern tablosundan okur. */
    private Process.Level detectLevelFromCode(String code, Map<String, Process.Level> levelPatterns) {
        if (code == null || code.isBlank()) return Process.Level.L1;
        String[] parts = code.split("[-.]");

        if (!levelPatterns.isEmpty()) {
            for (int i = 0; i < parts.length; i++) {
                Process.Level base = levelPatterns.get(parts[i].toUpperCase());
                if (base != null) {
                    int numericAfter = 0;
                    for (int j = i + 1; j < parts.length; j++) {
                        if (parts[j].matches("\\d+")) numericAfter++;
                    }
                    Process.Level[] levels = Process.Level.values();
                    int ordinal = Math.min(base.ordinal() + Math.max(0, numericAfter - 1), levels.length - 1);
                    return levels[ordinal];
                }
            }
        }

        // Fallback: sayısal segment sayısı
        int n = 0;
        for (String part : parts) if (part.matches("\\d+")) n++;
        return n <= 1 ? Process.Level.L1 : n == 2 ? Process.Level.L2 : Process.Level.L3;
    }

    // ─── Process CRUD ────────────────────────────────────────────

    @GetMapping
    public List<Map<String, Object>> list() {
        return repo.findAll().stream().map(this::toMap).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return repo.findById(id)
            .map(p -> ResponseEntity.ok(toMap(p)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Process p = new Process();
        p.setName((String) body.get("name"));
        p.setCode((String) body.get("code"));
        p.setShortDescription((String) body.getOrDefault("shortDescription", ""));
        p.setDescription((String) body.getOrDefault("description", ""));
        if (body.get("purpose") != null) p.setPurpose((String) body.get("purpose"));
        if (body.get("strategicGoal") != null) p.setStrategicGoal((String) body.get("strategicGoal"));
        if (body.get("strategicTarget") != null) p.setStrategicTarget((String) body.get("strategicTarget"));
        if (body.get("startPoint") != null) p.setStartPoint((String) body.get("startPoint"));
        if (body.get("endPoint") != null) p.setEndPoint((String) body.get("endPoint"));
        if (body.get("processScope") != null) p.setProcessScope((String) body.get("processScope"));
        if (body.get("affectedBy") != null) p.setAffectedBy((String) body.get("affectedBy"));
        if (body.get("affects") != null) p.setAffects((String) body.get("affects"));
        p.setActive(Boolean.TRUE.equals(body.getOrDefault("isActive", true)));
        if (body.get("level") != null)
            p.setLevel(Process.Level.valueOf((String) body.get("level")));
        if (body.get("processType") != null)
            p.setProcessType(Process.ProcessType.valueOf((String) body.get("processType")));
        if (body.get("criticality") != null)
            p.setCriticality(Process.Criticality.valueOf((String) body.get("criticality")));
        if (body.get("status") != null)
            p.setStatus(Process.Status.valueOf((String) body.get("status")));
        if (body.get("parentId") != null)
            repo.findById(Long.valueOf(body.get("parentId").toString())).ifPresent(p::setParent);
        if (body.get("ownerRoleId") != null)
            roleRepo.findById(Long.valueOf(body.get("ownerRoleId").toString())).ifPresent(p::setOwnerRole);
        if (body.get("ownerPositionId") != null)
            positionRepo.findById(Long.valueOf(body.get("ownerPositionId").toString())).ifPresent(p::setOwnerPosition);
        try {
            return ResponseEntity.ok(toMap(repo.save(p)));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).body(
                Map.of("message", "'" + p.getCode() + "' kodu zaten kullanımda. Farklı bir kod girin.")
            );
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(p -> {
            if (body.get("name") != null) p.setName((String) body.get("name"));
            if (body.get("code") != null) p.setCode((String) body.get("code"));
            if (body.get("shortDescription") != null) p.setShortDescription((String) body.get("shortDescription"));
            if (body.get("description") != null) p.setDescription((String) body.get("description"));
            if (body.containsKey("purpose")) p.setPurpose((String) body.get("purpose"));
            if (body.containsKey("strategicGoal")) p.setStrategicGoal((String) body.get("strategicGoal"));
            if (body.containsKey("strategicTarget")) p.setStrategicTarget((String) body.get("strategicTarget"));
            if (body.containsKey("startPoint")) p.setStartPoint((String) body.get("startPoint"));
            if (body.containsKey("endPoint")) p.setEndPoint((String) body.get("endPoint"));
            if (body.containsKey("processScope")) p.setProcessScope((String) body.get("processScope"));
            if (body.containsKey("affectedBy")) p.setAffectedBy((String) body.get("affectedBy"));
            if (body.containsKey("affects")) p.setAffects((String) body.get("affects"));
            if (body.get("isActive") != null) p.setActive(Boolean.TRUE.equals(body.get("isActive")));
            if (body.get("level") != null) p.setLevel(Process.Level.valueOf((String) body.get("level")));
            if (body.get("processType") != null) p.setProcessType(Process.ProcessType.valueOf((String) body.get("processType")));
            if (body.get("criticality") != null) p.setCriticality(Process.Criticality.valueOf((String) body.get("criticality")));
            if (body.get("status") != null) p.setStatus(Process.Status.valueOf((String) body.get("status")));
            if (body.containsKey("parentId")) {
                if (body.get("parentId") == null) p.setParent(null);
                else repo.findById(Long.valueOf(body.get("parentId").toString())).ifPresent(p::setParent);
            }
            if (body.get("ownerRoleId") != null)
                roleRepo.findById(Long.valueOf(body.get("ownerRoleId").toString())).ifPresent(p::setOwnerRole);
            if (body.containsKey("ownerPositionId")) {
                if (body.get("ownerPositionId") == null) p.setOwnerPosition(null);
                else positionRepo.findById(Long.valueOf(body.get("ownerPositionId").toString())).ifPresent(p::setOwnerPosition);
            }
            return ResponseEntity.ok(toMap(repo.save(p)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return repo.findById(id).map(p -> {
            repo.findAll().stream()
                .filter(c -> c.getParent() != null && c.getParent().getId().equals(id))
                .forEach(c -> { c.setParent(null); repo.save(c); });
            repo.deleteById(id);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // ─── Roles ───────────────────────────────────────────────────

    @GetMapping("/{id}/roles")
    public ResponseEntity<?> listRoles(@PathVariable Long id) {
        return repo.findById(id).map(p -> {
            List<Map<String, Object>> result = p.getOwnerRoles().stream().map(r -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", r.getId());
                m.put("name", r.getName());
                return m;
            }).toList();
            return ResponseEntity.ok(result);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/roles")
    public ResponseEntity<?> addRole(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(p -> {
            Long roleId = Long.valueOf(body.get("roleId").toString());
            return roleRepo.findById(roleId).map(role -> {
                boolean alreadyHas = p.getOwnerRoles().stream().anyMatch(r -> r.getId().equals(roleId));
                if (!alreadyHas) {
                    p.getOwnerRoles().add(role);
                    repo.save(p);
                }
                return ResponseEntity.ok().build();
            }).orElse(ResponseEntity.notFound().build());
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    public ResponseEntity<?> removeRole(@PathVariable Long id, @PathVariable Long roleId) {
        return repo.findById(id).map(p -> {
            p.getOwnerRoles().removeIf(r -> r.getId().equals(roleId));
            repo.save(p);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // ─── IOs ─────────────────────────────────────────────────────

    @GetMapping("/{id}/ios")
    public ResponseEntity<?> listIos(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ioRepo.findByProcessId(id).stream().map(this::ioToMap).toList());
    }

    @PostMapping("/{id}/ios")
    public ResponseEntity<?> addIo(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(p -> {
            ProcessIO io = new ProcessIO();
            io.setProcess(p);
            io.setName((String) body.get("name"));
            io.setIoType(ProcessIO.IOType.valueOf((String) body.get("ioType")));
            io.setSortOrder(body.get("sortOrder") != null ? Integer.parseInt(body.get("sortOrder").toString()) : 0);
            return ResponseEntity.ok(ioToMap(ioRepo.save(io)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/ios/{ioId}")
    public ResponseEntity<?> deleteIo(@PathVariable Long id, @PathVariable Long ioId) {
        if (!repo.existsById(id) || !ioRepo.existsById(ioId)) return ResponseEntity.notFound().build();
        ioRepo.deleteById(ioId);
        return ResponseEntity.noContent().build();
    }

    // ─── Parties ─────────────────────────────────────────────────

    @GetMapping("/{id}/parties")
    public ResponseEntity<?> listParties(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(partyRepo.findByProcessId(id).stream().map(this::partyToMap).toList());
    }

    @PostMapping("/{id}/parties")
    public ResponseEntity<?> addParty(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(p -> {
            ProcessParty party = new ProcessParty();
            party.setProcess(p);
            party.setName((String) body.get("name"));
            party.setPartyType(ProcessParty.PartyType.valueOf((String) body.get("partyType")));
            return ResponseEntity.ok(partyToMap(partyRepo.save(party)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/parties/{partyId}")
    public ResponseEntity<?> deleteParty(@PathVariable Long id, @PathVariable Long partyId) {
        if (!repo.existsById(id) || !partyRepo.existsById(partyId)) return ResponseEntity.notFound().build();
        partyRepo.deleteById(partyId);
        return ResponseEntity.noContent().build();
    }

    // ─── Steps ───────────────────────────────────────────────────

    @GetMapping("/{id}/steps")
    public ResponseEntity<?> listSteps(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(stepRepo.findByProcessIdOrderByStepNoAsc(id).stream().map(this::stepToMap).toList());
    }

    @PostMapping("/{id}/steps")
    public ResponseEntity<?> addStep(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(p -> {
            ProcessStep step = new ProcessStep();
            step.setProcess(p);
            step.setStepNo(body.get("stepNo") != null ? Integer.parseInt(body.get("stepNo").toString()) : 1);
            step.setName((String) body.get("name"));
            step.setDescription(body.get("description") != null ? (String) body.get("description") : "");
            return ResponseEntity.ok(stepToMap(stepRepo.save(step)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/steps/{stepId}")
    public ResponseEntity<?> updateStep(@PathVariable Long id, @PathVariable Long stepId, @RequestBody Map<String, Object> body) {
        return stepRepo.findById(stepId).map(step -> {
            if (body.get("stepNo") != null) step.setStepNo(Integer.parseInt(body.get("stepNo").toString()));
            if (body.get("name") != null) step.setName((String) body.get("name"));
            if (body.get("description") != null) step.setDescription((String) body.get("description"));
            return ResponseEntity.ok(stepToMap(stepRepo.save(step)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/steps/{stepId}")
    public ResponseEntity<?> deleteStep(@PathVariable Long id, @PathVariable Long stepId) {
        if (!repo.existsById(id) || !stepRepo.existsById(stepId)) return ResponseEntity.notFound().build();
        stepRepo.deleteById(stepId);
        return ResponseEntity.noContent().build();
    }

    // ─── Risks ───────────────────────────────────────────────────

    @GetMapping("/{id}/risks")
    public ResponseEntity<?> listRisks(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(riskRepo.findByProcessId(id).stream().map(this::riskToMap).toList());
    }

    @PostMapping("/{id}/risks")
    public ResponseEntity<?> addRisk(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(p -> {
            ProcessRisk risk = new ProcessRisk();
            risk.setProcess(p);
            risk.setName((String) body.get("name"));
            if (body.get("probability") != null)
                risk.setProbability(ProcessRisk.RiskLevel.valueOf((String) body.get("probability")));
            if (body.get("impact") != null)
                risk.setImpact(ProcessRisk.RiskLevel.valueOf((String) body.get("impact")));
            risk.setMitigation(body.get("mitigation") != null ? (String) body.get("mitigation") : "");
            return ResponseEntity.ok(riskToMap(riskRepo.save(risk)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/risks/{riskId}")
    public ResponseEntity<?> updateRisk(@PathVariable Long id, @PathVariable Long riskId, @RequestBody Map<String, Object> body) {
        return riskRepo.findById(riskId).map(risk -> {
            if (body.get("name") != null) risk.setName((String) body.get("name"));
            if (body.get("probability") != null) risk.setProbability(ProcessRisk.RiskLevel.valueOf((String) body.get("probability")));
            if (body.get("impact") != null) risk.setImpact(ProcessRisk.RiskLevel.valueOf((String) body.get("impact")));
            if (body.get("mitigation") != null) risk.setMitigation((String) body.get("mitigation"));
            return ResponseEntity.ok(riskToMap(riskRepo.save(risk)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/risks/{riskId}")
    public ResponseEntity<?> deleteRisk(@PathVariable Long id, @PathVariable Long riskId) {
        if (!repo.existsById(id) || !riskRepo.existsById(riskId)) return ResponseEntity.notFound().build();
        riskRepo.deleteById(riskId);
        return ResponseEntity.noContent().build();
    }

    // ─── Mappers ─────────────────────────────────────────────────

    private Map<String, Object> toMap(Process p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("code", p.getCode());
        m.put("shortDescription", p.getShortDescription() != null ? p.getShortDescription() : "");
        m.put("level", p.getLevel() != null ? p.getLevel().name() : "");
        m.put("processType", p.getProcessType() != null ? p.getProcessType().name() : "");
        m.put("criticality", p.getCriticality() != null ? p.getCriticality().name() : "");
        m.put("status", p.getStatus() != null ? p.getStatus().name() : "");
        m.put("isActive", p.isActive());
        m.put("parentId", p.getParent() != null ? p.getParent().getId() : null);
        m.put("parentName", p.getParent() != null ? p.getParent().getName() : "");
        m.put("ownerRoleId", p.getOwnerRole() != null ? p.getOwnerRole().getId() : null);
        m.put("ownerRoleName", p.getOwnerRole() != null ? p.getOwnerRole().getName() : "");
        m.put("ownerRoleIds", p.getOwnerRoles().stream().map(r -> r.getId()).toList());
        m.put("ownerRoleNames", p.getOwnerRoles().stream().map(r -> r.getName()).toList());
        m.put("ownerPositionId", p.getOwnerPosition() != null ? p.getOwnerPosition().getId() : null);
        m.put("ownerPositionName", p.getOwnerPosition() != null ? p.getOwnerPosition().getName() : "");
        m.put("description", p.getDescription() != null ? p.getDescription() : "");
        m.put("purpose", p.getPurpose() != null ? p.getPurpose() : "");
        m.put("strategicGoal", p.getStrategicGoal() != null ? p.getStrategicGoal() : "");
        m.put("strategicTarget", p.getStrategicTarget() != null ? p.getStrategicTarget() : "");
        m.put("startPoint", p.getStartPoint() != null ? p.getStartPoint() : "");
        m.put("endPoint", p.getEndPoint() != null ? p.getEndPoint() : "");
        m.put("processScope", p.getProcessScope() != null ? p.getProcessScope() : "");
        m.put("affectedBy", p.getAffectedBy() != null ? p.getAffectedBy() : "");
        m.put("affects", p.getAffects() != null ? p.getAffects() : "");
        m.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : "");
        return m;
    }

    private Map<String, Object> ioToMap(ProcessIO io) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", io.getId());
        m.put("name", io.getName());
        m.put("ioType", io.getIoType().name());
        m.put("sortOrder", io.getSortOrder());
        return m;
    }

    private Map<String, Object> partyToMap(ProcessParty party) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", party.getId());
        m.put("name", party.getName());
        m.put("partyType", party.getPartyType().name());
        return m;
    }

    private Map<String, Object> stepToMap(ProcessStep step) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", step.getId());
        m.put("stepNo", step.getStepNo());
        m.put("name", step.getName());
        m.put("description", step.getDescription() != null ? step.getDescription() : "");
        return m;
    }

    private Map<String, Object> riskToMap(ProcessRisk risk) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", risk.getId());
        m.put("name", risk.getName());
        m.put("probability", risk.getProbability() != null ? risk.getProbability().name() : "");
        m.put("impact", risk.getImpact() != null ? risk.getImpact().name() : "");
        m.put("mitigation", risk.getMitigation() != null ? risk.getMitigation() : "");
        return m;
    }

    // ─── KPIs ─────────────────────────────────────────────────────

    @GetMapping("/{id}/kpis")
    public ResponseEntity<?> listKpis(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(kpiRepo.findByProcessId(id).stream().map(this::kpiToMap).toList());
    }

    @PostMapping("/{id}/kpis")
    public ResponseEntity<?> addKpi(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(p -> {
            KPI kpi = new KPI();
            kpi.setProcess(p);
            kpi.setName((String) body.get("name"));
            kpi.setDefinition(body.get("definition") != null ? (String) body.get("definition") : "");
            kpi.setCalculationMethod(body.get("calculationMethod") != null ? (String) body.get("calculationMethod") : "");
            if (body.get("frequency") != null)
                kpi.setFrequency(KPI.Frequency.valueOf((String) body.get("frequency")));
            return ResponseEntity.ok(kpiToMap(kpiRepo.save(kpi)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/kpis/{kpiId}")
    public ResponseEntity<?> deleteKpi(@PathVariable Long id, @PathVariable Long kpiId) {
        if (!repo.existsById(id) || !kpiRepo.existsById(kpiId)) return ResponseEntity.notFound().build();
        kpiRepo.deleteById(kpiId);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> kpiToMap(KPI kpi) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", kpi.getId());
        m.put("name", kpi.getName());
        m.put("definition", kpi.getDefinition() != null ? kpi.getDefinition() : "");
        m.put("calculationMethod", kpi.getCalculationMethod() != null ? kpi.getCalculationMethod() : "");
        m.put("frequency", kpi.getFrequency() != null ? kpi.getFrequency().name() : "");
        return m;
    }

    // ─── Stakeholders ─────────────────────────────────────────────

    @GetMapping("/{id}/stakeholders")
    public ResponseEntity<?> listStakeholders(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(stakeholderRepo.findByProcessId(id).stream().map(this::stakeholderToMap).toList());
    }

    @PostMapping("/{id}/stakeholders")
    public ResponseEntity<?> addStakeholder(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(p -> {
            ProcessStakeholder sh = new ProcessStakeholder();
            sh.setProcess(p);
            sh.setName((String) body.get("name"));
            sh.setStakeholderType(ProcessStakeholder.StakeholderType.valueOf((String) body.get("stakeholderType")));
            return ResponseEntity.ok(stakeholderToMap(stakeholderRepo.save(sh)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/stakeholders/{stakeholderId}")
    public ResponseEntity<?> deleteStakeholder(@PathVariable Long id, @PathVariable Long stakeholderId) {
        if (!repo.existsById(id) || !stakeholderRepo.existsById(stakeholderId)) return ResponseEntity.notFound().build();
        stakeholderRepo.deleteById(stakeholderId);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> stakeholderToMap(ProcessStakeholder sh) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", sh.getId());
        m.put("name", sh.getName());
        m.put("stakeholderType", sh.getStakeholderType().name());
        return m;
    }
}
