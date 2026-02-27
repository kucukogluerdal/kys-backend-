package com.kys.processes.controller;

import com.kys.organization.repository.RoleRepository;
import com.kys.processes.entity.KPI;
import com.kys.processes.repository.KPIRepository;
import com.kys.processes.repository.ProcessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/processes/kpis")
@RequiredArgsConstructor
public class KPIController {

    private final KPIRepository repo;
    private final ProcessRepository processRepo;
    private final RoleRepository roleRepo;

    @GetMapping
    public List<Map<String, Object>> list() {
        return repo.findAll().stream().map(this::toMap).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return repo.findById(id)
            .map(k -> ResponseEntity.ok(toMap(k)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        KPI k = new KPI();
        k.setName((String) body.get("name"));
        k.setDefinition((String) body.getOrDefault("definition", ""));
        k.setCalculationMethod((String) body.getOrDefault("calculationMethod", ""));
        k.setTargetValue((String) body.getOrDefault("targetValue", ""));
        k.setThresholds((String) body.getOrDefault("thresholds", ""));
        k.setActive(Boolean.TRUE.equals(body.getOrDefault("isActive", true)));
        if (body.get("frequency") != null)
            k.setFrequency(KPI.Frequency.valueOf((String) body.get("frequency")));
        if (body.get("processId") != null)
            processRepo.findById(Long.valueOf(body.get("processId").toString())).ifPresent(k::setProcess);
        if (body.get("ownerRoleId") != null)
            roleRepo.findById(Long.valueOf(body.get("ownerRoleId").toString())).ifPresent(k::setOwnerRole);
        return ResponseEntity.ok(toMap(repo.save(k)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(k -> {
            if (body.get("name") != null) k.setName((String) body.get("name"));
            if (body.get("definition") != null) k.setDefinition((String) body.get("definition"));
            if (body.get("calculationMethod") != null) k.setCalculationMethod((String) body.get("calculationMethod"));
            if (body.get("targetValue") != null) k.setTargetValue((String) body.get("targetValue"));
            if (body.get("thresholds") != null) k.setThresholds((String) body.get("thresholds"));
            if (body.get("isActive") != null) k.setActive(Boolean.TRUE.equals(body.get("isActive")));
            if (body.get("frequency") != null) k.setFrequency(KPI.Frequency.valueOf((String) body.get("frequency")));
            if (body.get("processId") != null)
                processRepo.findById(Long.valueOf(body.get("processId").toString())).ifPresent(k::setProcess);
            if (body.get("ownerRoleId") != null)
                roleRepo.findById(Long.valueOf(body.get("ownerRoleId").toString())).ifPresent(k::setOwnerRole);
            return ResponseEntity.ok(toMap(repo.save(k)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toMap(KPI k) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", k.getId());
        m.put("name", k.getName());
        m.put("definition", k.getDefinition() != null ? k.getDefinition() : "");
        m.put("calculationMethod", k.getCalculationMethod() != null ? k.getCalculationMethod() : "");
        m.put("targetValue", k.getTargetValue() != null ? k.getTargetValue() : "");
        m.put("thresholds", k.getThresholds() != null ? k.getThresholds() : "");
        m.put("frequency", k.getFrequency() != null ? k.getFrequency().name() : "");
        m.put("isActive", k.isActive());
        m.put("processId", k.getProcess() != null ? k.getProcess().getId() : null);
        m.put("processName", k.getProcess() != null ? k.getProcess().getName() : "");
        m.put("processCode", k.getProcess() != null ? k.getProcess().getCode() : "");
        m.put("ownerRoleId", k.getOwnerRole() != null ? k.getOwnerRole().getId() : null);
        m.put("ownerRoleName", k.getOwnerRole() != null ? k.getOwnerRole().getName() : "");
        return m;
    }
}
