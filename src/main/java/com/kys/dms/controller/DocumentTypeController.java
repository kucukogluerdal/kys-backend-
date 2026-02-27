package com.kys.dms.controller;

import com.kys.dms.entity.DocumentType;
import com.kys.dms.repository.DocumentTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dms/types")
@RequiredArgsConstructor
public class DocumentTypeController {

    private final DocumentTypeRepository repo;

    @GetMapping
    public List<Map<String, Object>> list() {
        return repo.findAll().stream().map(this::toMap).toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        DocumentType t = new DocumentType();
        t.setCode((String) body.get("code"));
        t.setName((String) body.get("name"));
        t.setDescription((String) body.getOrDefault("description", ""));
        t.setLevel(body.get("level") != null ? Integer.parseInt(body.get("level").toString()) : 1);
        t.setActive(Boolean.TRUE.equals(body.getOrDefault("isActive", true)));
        return ResponseEntity.ok(toMap(repo.save(t)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(t -> {
            if (body.get("code") != null) t.setCode((String) body.get("code"));
            if (body.get("name") != null) t.setName((String) body.get("name"));
            if (body.get("description") != null) t.setDescription((String) body.get("description"));
            if (body.get("level") != null) t.setLevel(Integer.parseInt(body.get("level").toString()));
            if (body.get("isActive") != null) t.setActive(Boolean.TRUE.equals(body.get("isActive")));
            return ResponseEntity.ok(toMap(repo.save(t)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toMap(DocumentType t) {
        return Map.of(
            "id", t.getId(), "code", t.getCode(), "name", t.getName(),
            "level", t.getLevel(),
            "description", t.getDescription() != null ? t.getDescription() : "",
            "isActive", t.isActive()
        );
    }
}
