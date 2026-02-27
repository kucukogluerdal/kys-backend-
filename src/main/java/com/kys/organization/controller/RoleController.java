package com.kys.organization.controller;

import com.kys.organization.entity.Role;
import com.kys.organization.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/organization/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleRepository repo;

    @GetMapping
    public List<Map<String, Object>> list() {
        return repo.findAll().stream().map(this::toMap).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return repo.findById(id)
            .map(r -> ResponseEntity.ok(toMap(r)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Role role = new Role();
        role.setName((String) body.get("name"));
        role.setCode((String) body.get("code"));
        role.setDescription((String) body.getOrDefault("description", ""));
        role.setActive(Boolean.TRUE.equals(body.getOrDefault("isActive", true)));
        if (body.get("roleType") != null) {
            role.setRoleType(Role.RoleType.valueOf((String) body.get("roleType")));
        }
        return ResponseEntity.ok(toMap(repo.save(role)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(role -> {
            if (body.get("name") != null) role.setName((String) body.get("name"));
            if (body.get("code") != null) role.setCode((String) body.get("code"));
            if (body.get("description") != null) role.setDescription((String) body.get("description"));
            if (body.get("isActive") != null) role.setActive(Boolean.TRUE.equals(body.get("isActive")));
            if (body.get("roleType") != null) role.setRoleType(Role.RoleType.valueOf((String) body.get("roleType")));
            return ResponseEntity.ok(toMap(repo.save(role)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toMap(Role r) {
        return Map.of(
            "id", r.getId(),
            "name", r.getName(),
            "code", r.getCode(),
            "roleType", r.getRoleType() != null ? r.getRoleType().name() : "",
            "description", r.getDescription() != null ? r.getDescription() : "",
            "isActive", r.isActive()
        );
    }
}
