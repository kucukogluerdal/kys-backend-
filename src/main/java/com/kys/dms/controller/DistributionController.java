package com.kys.dms.controller;

import com.kys.dms.entity.Distribution;
import com.kys.dms.repository.DistributionRepository;
import com.kys.dms.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dms/distributions")
@RequiredArgsConstructor
public class DistributionController {

    private final DistributionRepository repo;
    private final DocumentRepository documentRepo;

    @GetMapping
    public List<Map<String, Object>> list() {
        return repo.findAll().stream().map(this::toMap).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return repo.findById(id)
            .map(d -> ResponseEntity.ok(toMap(d)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Distribution d = new Distribution();
        if (body.get("documentId") != null)
            documentRepo.findById(Long.valueOf(body.get("documentId").toString())).ifPresent(d::setDocument);
        if (body.get("distributionType") != null)
            d.setDistributionType(Distribution.DistributionType.valueOf((String) body.get("distributionType")));
        d.setNotes((String) body.getOrDefault("notes", ""));
        return ResponseEntity.ok(toMap(repo.save(d)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toMap(Distribution d) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", d.getId());
        m.put("documentId", d.getDocument() != null ? d.getDocument().getId() : null);
        m.put("documentCode", d.getDocument() != null ? d.getDocument().getCode() : "");
        m.put("documentTitle", d.getDocument() != null ? d.getDocument().getTitle() : "");
        m.put("distributionType", d.getDistributionType() != null ? d.getDistributionType().name() : "");
        m.put("notes", d.getNotes() != null ? d.getNotes() : "");
        m.put("distributedAt", d.getDistributedAt() != null ? d.getDistributedAt().toString() : "");
        return m;
    }
}
