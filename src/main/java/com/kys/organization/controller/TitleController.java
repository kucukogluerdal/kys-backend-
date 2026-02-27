package com.kys.organization.controller;

import com.kys.organization.entity.Title;
import com.kys.organization.repository.TitleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/organization/titles")
@RequiredArgsConstructor
public class TitleController {

    private final TitleRepository repo;

    @GetMapping
    public List<Map<String, Object>> list() {
        return repo.findAll().stream().map(this::toMap).toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Title t = new Title();
        t.setName((String) body.get("name"));
        t.setCode((String) body.get("code"));
        t.setActive(Boolean.TRUE.equals(body.getOrDefault("isActive", true)));
        return ResponseEntity.ok(toMap(repo.save(t)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(t -> {
            if (body.get("name") != null) t.setName((String) body.get("name"));
            if (body.get("code") != null) t.setCode((String) body.get("code"));
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

    private Map<String, Object> toMap(Title t) {
        return Map.of("id", t.getId(), "name", t.getName(), "code", t.getCode(), "isActive", t.isActive());
    }
}
