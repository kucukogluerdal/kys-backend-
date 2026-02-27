package com.kys.dms.controller;

import com.kys.dms.entity.DocumentStatus;
import com.kys.dms.repository.DocumentStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dms/statuses")
@RequiredArgsConstructor
public class DocumentStatusController {

    private final DocumentStatusRepository repo;

    @GetMapping
    public List<Map<String, Object>> list() {
        return repo.findAll().stream()
            .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
            .map(this::toMap).toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        DocumentStatus s = new DocumentStatus();
        s.setCode((String) body.get("code"));
        s.setName((String) body.get("name"));
        s.setOrder(body.get("order") != null ? Integer.parseInt(body.get("order").toString()) : 0);
        s.setActive(Boolean.TRUE.equals(body.getOrDefault("isActive", true)));
        return ResponseEntity.ok(toMap(repo.save(s)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(s -> {
            if (body.get("code") != null) s.setCode((String) body.get("code"));
            if (body.get("name") != null) s.setName((String) body.get("name"));
            if (body.get("order") != null) s.setOrder(Integer.parseInt(body.get("order").toString()));
            if (body.get("isActive") != null) s.setActive(Boolean.TRUE.equals(body.get("isActive")));
            return ResponseEntity.ok(toMap(repo.save(s)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toMap(DocumentStatus s) {
        return Map.of(
            "id", s.getId(), "code", s.getCode(),
            "name", s.getName(), "order", s.getOrder(), "isActive", s.isActive()
        );
    }
}
