package com.kys.dms.controller;

import com.kys.auth.UserRepository;
import com.kys.dms.entity.Document;
import com.kys.dms.entity.DocumentRevision;
import com.kys.dms.repository.DocumentRepository;
import com.kys.dms.repository.DocumentStatusRepository;
import com.kys.dms.repository.DocumentTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/dms/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentRepository repo;
    private final DocumentTypeRepository typeRepo;
    private final DocumentStatusRepository statusRepo;
    private final UserRepository userRepo;

    @GetMapping
    public List<Map<String, Object>> list() {
        return repo.findAll().stream().map(this::toMap).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return repo.findById(id)
            .map(d -> ResponseEntity.ok(toDetailMap(d)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Document d = new Document();
        d.setCode((String) body.get("code"));
        d.setTitle((String) body.get("title"));
        if (body.get("docTypeId") != null)
            typeRepo.findById(Long.valueOf(body.get("docTypeId").toString())).ifPresent(d::setDocType);
        if (body.get("statusId") != null)
            statusRepo.findById(Long.valueOf(body.get("statusId").toString())).ifPresent(d::setStatus);
        if (body.get("parentId") != null)
            repo.findById(Long.valueOf(body.get("parentId").toString())).ifPresent(d::setParent);
        if (body.get("createdById") != null)
            userRepo.findById(Long.valueOf(body.get("createdById").toString())).ifPresent(d::setCreatedBy);
        return ResponseEntity.ok(toMap(repo.save(d)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(d -> {
            if (body.get("code") != null) d.setCode((String) body.get("code"));
            if (body.get("title") != null) d.setTitle((String) body.get("title"));
            if (body.get("docTypeId") != null)
                typeRepo.findById(Long.valueOf(body.get("docTypeId").toString())).ifPresent(d::setDocType);
            if (body.get("statusId") != null)
                statusRepo.findById(Long.valueOf(body.get("statusId").toString())).ifPresent(d::setStatus);
            return ResponseEntity.ok(toMap(repo.save(d)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/upload")
    public ResponseEntity<?> uploadFile(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        return repo.findById(id).map(d -> {
            try {
                Path uploadDir = Paths.get("uploads/documents");
                Files.createDirectories(uploadDir);
                String storedName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Path target = uploadDir.resolve(storedName);
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                d.setFilePath(storedName);
                d.setOriginalFilename(file.getOriginalFilename());
                repo.save(d);
                return ResponseEntity.ok(Map.of("filePath", storedName, "originalFilename", file.getOriginalFilename()));
            } catch (IOException e) {
                return ResponseEntity.internalServerError().body(Map.of("message", "Dosya kaydedilemedi"));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        var docOpt = repo.findById(id);
        if (docOpt.isEmpty() || docOpt.get().getFilePath() == null)
            return ResponseEntity.notFound().build();
        try {
            Document d = docOpt.get();
            Path file = Paths.get("uploads/documents").resolve(d.getFilePath());
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists()) return ResponseEntity.notFound().build();
            String downloadName = d.getOriginalFilename() != null ? d.getOriginalFilename() : d.getFilePath();
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Map<String, Object> toMap(Document d) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", d.getId());
        m.put("code", d.getCode());
        m.put("title", d.getTitle());
        m.put("docTypeId", d.getDocType() != null ? d.getDocType().getId() : null);
        m.put("docTypeName", d.getDocType() != null ? d.getDocType().getName() : "");
        m.put("statusId", d.getStatus() != null ? d.getStatus().getId() : null);
        m.put("statusName", d.getStatus() != null ? d.getStatus().getName() : "");
        m.put("parentId", d.getParent() != null ? d.getParent().getId() : null);
        m.put("createdById", d.getCreatedBy() != null ? d.getCreatedBy().getId() : null);
        m.put("filePath", d.getFilePath());
        m.put("originalFilename", d.getOriginalFilename());
        m.put("revisionCount", d.getRevisions().size());
        m.put("createdAt", d.getCreatedAt() != null ? d.getCreatedAt().toString() : "");
        return m;
    }

    private Map<String, Object> toDetailMap(Document d) {
        Map<String, Object> m = toMap(d);
        List<Map<String, Object>> revisions = d.getRevisions().stream().map(r -> {
            Map<String, Object> rm = new HashMap<>();
            rm.put("id", r.getId());
            rm.put("revisionNote", r.getRevisionNote() != null ? r.getRevisionNote() : "");
            rm.put("revisedAt", r.getRevisedAt() != null ? r.getRevisedAt().toString() : "");
            rm.put("revisedBy", r.getRevisedBy() != null ? r.getRevisedBy().getUsername() : "");
            return rm;
        }).toList();
        m.put("revisions", revisions);
        return m;
    }
}
