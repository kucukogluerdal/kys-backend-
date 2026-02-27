package com.kys.organization.controller;

import com.kys.organization.entity.Position;
import com.kys.organization.entity.Role;
import com.kys.organization.repository.OrgUnitRepository;
import com.kys.organization.repository.PositionRepository;
import com.kys.organization.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/organization/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionRepository repo;
    private final OrgUnitRepository unitRepo;
    private final RoleRepository roleRepo;

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

    @GetMapping("/{id}/detail")
    @Transactional
    public ResponseEntity<?> detail(@PathVariable Long id) {
        return repo.findById(id).map(p -> {
            Map<String, Object> result = new LinkedHashMap<>(toMap(p));

            List<Map<String, Object>> units = unitRepo.findByPositionsId(p.getId()).stream()
                .sorted(Comparator.comparing(u -> u.getName()))
                .map(u -> Map.<String, Object>of("id", u.getId(), "name", u.getName(), "code", u.getCode()))
                .toList();
            result.put("units", units);

            List<Map<String, Object>> roles = p.getRoles().stream()
                .sorted(Comparator.comparing(Role::getName))
                .map(r -> {
                    Map<String, Object> rm = new LinkedHashMap<>();
                    rm.put("id", r.getId());
                    rm.put("name", r.getName());
                    rm.put("code", r.getCode());
                    rm.put("roleType", r.getRoleType() != null ? r.getRoleType().name() : "");
                    return rm;
                }).toList();
            result.put("roles", roles);

            return ResponseEntity.ok(result);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/roles")
    @Transactional
    public ResponseEntity<?> assignRoles(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        var posOpt = repo.findById(id);
        if (posOpt.isEmpty()) return ResponseEntity.notFound().build();
        Position pos = posOpt.get();
        List<Integer> roleIds = (List<Integer>) body.get("roleIds");
        for (Integer rid : roleIds) {
            roleRepo.findById(rid.longValue()).ifPresent(role -> {
                if (!pos.getRoles().contains(role)) pos.getRoles().add(role);
            });
        }
        repo.save(pos);
        return ResponseEntity.ok(Map.of("message", "Roller atandı"));
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    @Transactional
    public ResponseEntity<?> removeRole(@PathVariable Long id, @PathVariable Long roleId) {
        return repo.findById(id).map(pos -> {
            pos.getRoles().removeIf(r -> r.getId().equals(roleId));
            repo.save(pos);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/units")
    @Transactional
    public ResponseEntity<?> assignUnits(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        var posOpt = repo.findById(id);
        if (posOpt.isEmpty()) return ResponseEntity.notFound().build();
        Position pos = posOpt.get();
        List<Integer> unitIds = (List<Integer>) body.get("unitIds");
        for (Integer uid : unitIds) {
            unitRepo.findById(uid.longValue()).ifPresent(unit -> {
                if (!unit.getPositions().contains(pos)) {
                    unit.getPositions().add(pos);
                    unitRepo.save(unit);
                }
            });
        }
        return ResponseEntity.ok(Map.of("message", "Birimler atandı"));
    }

    @DeleteMapping("/{id}/units/{unitId}")
    @Transactional
    public ResponseEntity<?> removeUnit(@PathVariable Long id, @PathVariable Long unitId) {
        return unitRepo.findById(unitId).map(unit -> {
            unit.getPositions().removeIf(p -> p.getId().equals(id));
            unitRepo.save(unit);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Position p = new Position();
        p.setName((String) body.get("name"));
        p.setCode((String) body.get("code"));
        p.setDescription((String) body.getOrDefault("description", ""));
        p.setActive(Boolean.TRUE.equals(body.getOrDefault("isActive", true)));
        if (body.get("level") != null) {
            p.setLevel(Position.Level.valueOf((String) body.get("level")));
        }
        return ResponseEntity.ok(toMap(repo.save(p)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(p -> {
            if (body.get("name") != null) p.setName((String) body.get("name"));
            if (body.get("code") != null) p.setCode((String) body.get("code"));
            if (body.get("description") != null) p.setDescription((String) body.get("description"));
            if (body.get("isActive") != null) p.setActive(Boolean.TRUE.equals(body.get("isActive")));
            if (body.get("level") != null) p.setLevel(Position.Level.valueOf((String) body.get("level")));
            return ResponseEntity.ok(toMap(repo.save(p)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toMap(Position p) {
        return Map.of(
            "id", p.getId(),
            "name", p.getName(),
            "code", p.getCode(),
            "level", p.getLevel() != null ? p.getLevel().name() : "",
            "description", p.getDescription() != null ? p.getDescription() : "",
            "isActive", p.isActive()
        );
    }
}
