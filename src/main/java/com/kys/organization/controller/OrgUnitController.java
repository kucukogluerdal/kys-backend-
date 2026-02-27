package com.kys.organization.controller;

import com.kys.organization.entity.OrgUnit;
import com.kys.organization.entity.Position;
import com.kys.organization.repository.OrgUnitRepository;
import com.kys.organization.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/organization/units")
@RequiredArgsConstructor
public class OrgUnitController {

    private final OrgUnitRepository repo;
    private final PositionRepository positionRepo;

    @GetMapping
    @Transactional
    public List<Map<String, Object>> list() {
        return repo.findAll().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>(toMap(u));
            m.put("childCount", u.getChildren().size());
            m.put("positionCount", u.getPositions().size());
            return m;
        }).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return repo.findById(id)
            .map(u -> ResponseEntity.ok(toMap(u)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/detail")
    @Transactional
    public ResponseEntity<?> detail(@PathVariable Long id) {
        return repo.findById(id).map(u -> {
            Map<String, Object> result = new LinkedHashMap<>(toMap(u));

            List<Map<String, Object>> children = u.getChildren().stream()
                .map(c -> Map.<String, Object>of(
                    "id", c.getId(), "name", c.getName(), "code", c.getCode()))
                .toList();
            result.put("children", children);

            List<Map<String, Object>> positions = u.getPositions().stream()
                .map(p -> {
                    Map<String, Object> pm = new LinkedHashMap<>();
                    pm.put("id", p.getId());
                    pm.put("name", p.getName());
                    pm.put("code", p.getCode());
                    pm.put("level", p.getLevel() != null ? p.getLevel().name() : "");
                    pm.put("description", p.getDescription() != null ? p.getDescription() : "");
                    pm.put("roles", p.getRoles().stream()
                        .map(r -> Map.<String, Object>of("id", r.getId(), "name", r.getName(), "code", r.getCode()))
                        .toList());
                    return pm;
                }).toList();
            result.put("positions", positions);

            return ResponseEntity.ok(result);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        OrgUnit unit = new OrgUnit();
        unit.setName((String) body.get("name"));
        unit.setCode((String) body.get("code"));
        unit.setActive(Boolean.TRUE.equals(body.getOrDefault("isActive", true)));
        if (body.get("parentId") != null)
            repo.findById(Long.valueOf(body.get("parentId").toString())).ifPresent(unit::setParent);
        return ResponseEntity.ok(toMap(repo.save(unit)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(unit -> {
            if (body.get("name") != null) unit.setName((String) body.get("name"));
            if (body.get("code") != null) unit.setCode((String) body.get("code"));
            if (body.get("isActive") != null) unit.setActive(Boolean.TRUE.equals(body.get("isActive")));
            if (body.containsKey("parentId")) {
                if (body.get("parentId") == null) unit.setParent(null);
                else repo.findById(Long.valueOf(body.get("parentId").toString())).ifPresent(unit::setParent);
            }
            return ResponseEntity.ok(toMap(repo.save(unit)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Pozisyon Atamaları ────────────────────────────────────────
    @PostMapping("/{id}/positions")
    @Transactional
    public ResponseEntity<?> assignPositions(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        var unitOpt = repo.findById(id);
        if (unitOpt.isEmpty()) return ResponseEntity.notFound().build();
        OrgUnit unit = unitOpt.get();
        List<Integer> positionIds = (List<Integer>) body.get("positionIds");
        for (Integer pid : positionIds) {
            positionRepo.findById(pid.longValue()).ifPresent(pos -> {
                if (!unit.getPositions().contains(pos)) {
                    unit.getPositions().add(pos);
                }
            });
        }
        repo.save(unit);
        return ResponseEntity.ok(Map.of("message", "Pozisyonlar atandı"));
    }

    @PostMapping("/{id}/positions/{positionId}")
    @Transactional
    public ResponseEntity<?> assignPosition(@PathVariable Long id, @PathVariable Long positionId) {
        var unitOpt = repo.findById(id);
        var posOpt = positionRepo.findById(positionId);
        if (unitOpt.isEmpty() || posOpt.isEmpty()) return ResponseEntity.notFound().build();
        OrgUnit unit = unitOpt.get();
        Position pos = posOpt.get();
        if (!unit.getPositions().contains(pos)) {
            unit.getPositions().add(pos);
            repo.save(unit);
        }
        return ResponseEntity.ok(Map.of("message", "Pozisyon atandı"));
    }

    @DeleteMapping("/{id}/positions/{positionId}")
    @Transactional
    public ResponseEntity<?> removePosition(@PathVariable Long id, @PathVariable Long positionId) {
        var unitOpt = repo.findById(id);
        if (unitOpt.isEmpty()) return ResponseEntity.notFound().build();
        OrgUnit unit = unitOpt.get();
        unit.getPositions().removeIf(p -> p.getId().equals(positionId));
        repo.save(unit);
        return ResponseEntity.noContent().build();
    }

    // ─── Alt Birim Yönetimi ───────────────────────────────────────
    @PostMapping("/{id}/children/{childId}")
    public ResponseEntity<?> addChild(@PathVariable Long id, @PathVariable Long childId) {
        var parentOpt = repo.findById(id);
        var childOpt = repo.findById(childId);
        if (parentOpt.isEmpty() || childOpt.isEmpty()) return ResponseEntity.notFound().build();
        OrgUnit child = childOpt.get();
        child.setParent(parentOpt.get());
        return ResponseEntity.ok(toMap(repo.save(child)));
    }

    @DeleteMapping("/{id}/children/{childId}")
    public ResponseEntity<?> removeChild(@PathVariable Long id, @PathVariable Long childId) {
        return repo.findById(childId).map(child -> {
            if (child.getParent() != null && child.getParent().getId().equals(id)) {
                child.setParent(null);
                repo.save(child);
            }
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> toMap(OrgUnit u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("name", u.getName());
        m.put("code", u.getCode());
        m.put("isActive", u.isActive());
        m.put("parentId", u.getParent() != null ? u.getParent().getId() : null);
        m.put("parentName", u.getParent() != null ? u.getParent().getName() : "");
        return m;
    }
}
