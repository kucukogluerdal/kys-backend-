package com.kys.processes.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kys.processes.entity.ProcessCodeConfig;
import com.kys.processes.repository.ProcessCodeConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/process-code-config")
@RequiredArgsConstructor
public class ProcessCodeConfigController {

    private final ProcessCodeConfigRepository repo;
    private final ObjectMapper objectMapper;

    // ─── GET: mevcut config ──────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> get() {
        Optional<ProcessCodeConfig> opt = repo.findFirstByOrderByIdAsc();
        if (opt.isEmpty()) return ResponseEntity.ok(Map.of());
        return ResponseEntity.ok(toMap(opt.get()));
    }

    // ─── POST: upsert (oluştur veya güncelle) ────────────────────
    @PostMapping
    @Transactional
    public ResponseEntity<?> save(@RequestBody Map<String, Object> body) {
        try {
            ProcessCodeConfig cfg = repo.findFirstByOrderByIdAsc()
                    .orElse(new ProcessCodeConfig());

            if (body.containsKey("separator"))
                cfg.setSeparator((String) body.get("separator"));

            if (body.containsKey("fieldNames"))
                cfg.setFieldNamesJson(objectMapper.writeValueAsString(body.get("fieldNames")));

            if (body.containsKey("levelRules"))
                cfg.setLevelRulesJson(objectMapper.writeValueAsString(body.get("levelRules")));

            if (body.containsKey("parentTemplateL2"))
                cfg.setParentTemplateL2((String) body.get("parentTemplateL2"));

            if (body.containsKey("parentTemplateL3"))
                cfg.setParentTemplateL3((String) body.get("parentTemplateL3"));

            return ResponseEntity.ok(toMap(repo.save(cfg)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─── DELETE: config'i sıfırla ────────────────────────────────
    @DeleteMapping
    @Transactional
    public ResponseEntity<?> delete() {
        repo.findFirstByOrderByIdAsc().ifPresent(repo::delete);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    // ─── POST /test: örnek kod ile seviye + parent hesapla ───────
    @PostMapping("/test")
    public ResponseEntity<?> test(@RequestBody Map<String, Object> body) {
        String rawCode = (String) body.get("code");
        if (rawCode == null || rawCode.isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "code zorunlu"));

        Optional<ProcessCodeConfig> opt = repo.findFirstByOrderByIdAsc();
        if (opt.isEmpty())
            return ResponseEntity.ok(Map.of("error", "Config tanımlı değil"));

        ProcessCodeConfig cfg = opt.get();
        String sep = cfg.getSeparator() != null ? cfg.getSeparator() : "-";

        try {
            List<String> fieldNames = cfg.getFieldNamesJson() != null
                    ? objectMapper.readValue(cfg.getFieldNamesJson(), new TypeReference<>() {})
                    : List.of();

            List<Map<String, Object>> rules = cfg.getLevelRulesJson() != null
                    ? objectMapper.readValue(cfg.getLevelRulesJson(), new TypeReference<>() {})
                    : List.of();

            String code = rawCode.trim();
            String[] tokens = code.split(Pattern.quote(sep));

            // Alan haritası
            Map<String, String> fields = new LinkedHashMap<>();
            for (int i = 0; i < fieldNames.size() && i < tokens.length; i++) {
                fields.put(fieldNames.get(i), tokens[i]);
            }

            // Seviye tespiti
            String level = applyLevelRules(tokens, rules);

            // Parent kodu
            String template = "L2".equals(level) ? cfg.getParentTemplateL2()
                    : "L3".equals(level) ? cfg.getParentTemplateL3() : null;
            String parentCode = expandTemplate(template, fields);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("rawCode", code);
            result.put("tokens", tokens);
            result.put("fields", fields);
            result.put("level", level != null ? level : "—");
            result.put("parentCode", parentCode != null ? parentCode : "");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("error", e.getMessage()));
        }
    }

    // ─── Yardımcılar ─────────────────────────────────────────────

    private Map<String, Object> toMap(ProcessCodeConfig cfg) {
        try {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", cfg.getId());
            m.put("separator", cfg.getSeparator());
            m.put("fieldNames", cfg.getFieldNamesJson() != null
                    ? objectMapper.readValue(cfg.getFieldNamesJson(), new TypeReference<List<String>>() {})
                    : List.of());
            m.put("levelRules", cfg.getLevelRulesJson() != null
                    ? objectMapper.readValue(cfg.getLevelRulesJson(), new TypeReference<List<Map<String, Object>>>() {})
                    : List.of());
            m.put("parentTemplateL2", cfg.getParentTemplateL2() != null ? cfg.getParentTemplateL2() : "");
            m.put("parentTemplateL3", cfg.getParentTemplateL3() != null ? cfg.getParentTemplateL3() : "");
            return m;
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    public static String applyLevelRules(String[] tokens, List<Map<String, Object>> rules) {
        for (Map<String, Object> rule : rules) {
            int count = rule.containsKey("count") ? ((Number) rule.get("count")).intValue() : -1;
            int index = rule.containsKey("index") ? ((Number) rule.get("index")).intValue() : -1;
            String value = (String) rule.get("value");
            String level = (String) rule.get("level");

            if (count >= 0 && tokens.length != count) continue;
            if (index >= 0 && value != null) {
                if (index >= tokens.length || !value.equalsIgnoreCase(tokens[index])) continue;
            }
            return level;
        }
        return null;
    }

    public static String expandTemplate(String template, Map<String, String> fields) {
        if (template == null || template.isBlank()) return null;
        String result = template;
        for (Map.Entry<String, String> e : fields.entrySet()) {
            result = result.replace("{" + e.getKey() + "}", e.getValue());
        }
        return result;
    }
}
