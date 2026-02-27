package com.kys.processes.controller;

import com.kys.processes.entity.ProcessLevelPattern;
import com.kys.processes.repository.ProcessLevelPatternRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/process-level-patterns")
@RequiredArgsConstructor
public class ProcessLevelPatternController {

    private final ProcessLevelPatternRepository repo;

    @GetMapping
    public List<ProcessLevelPattern> getAll() {
        return repo.findAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ProcessLevelPattern p) {
        p.setId(null);
        p.setSegment(p.getSegment().toUpperCase().trim());
        if (p.getSegment().isBlank() || p.getBaseLevel() == null)
            return ResponseEntity.badRequest().body("segment ve baseLevel zorunludur");
        return ResponseEntity.ok(repo.save(p));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ProcessLevelPattern p) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        p.setId(id);
        p.setSegment(p.getSegment().toUpperCase().trim());
        return ResponseEntity.ok(repo.save(p));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
