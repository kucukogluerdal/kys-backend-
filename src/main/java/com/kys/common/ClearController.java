package com.kys.common;

import com.kys.dms.repository.DocumentRepository;
import com.kys.dms.repository.DocumentStatusRepository;
import com.kys.dms.repository.DocumentTypeRepository;
import com.kys.organization.repository.*;
import com.kys.processes.repository.KPIRepository;
import com.kys.processes.repository.ProcessRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/clear")
@RequiredArgsConstructor
public class ClearController {

    private final UserProfileRepository userProfileRepo;
    private final OrgUnitRepository unitRepo;
    private final PositionRepository positionRepo;
    private final RoleRepository roleRepo;
    private final TitleRepository titleRepo;
    private final DocumentRepository documentRepo;
    private final DocumentTypeRepository docTypeRepo;
    private final DocumentStatusRepository docStatusRepo;
    private final ProcessRepository processRepo;
    private final KPIRepository kpiRepo;
    private final EntityManager em;

    @DeleteMapping("/users")
    @Transactional
    public ResponseEntity<?> clearUsers() {
        long count = userProfileRepo.count();
        userProfileRepo.deleteAll();
        return ok(count);
    }

    @DeleteMapping("/titles")
    @Transactional
    public ResponseEntity<?> clearTitles() {
        em.createNativeQuery("UPDATE user_profiles SET title_id = NULL").executeUpdate();
        em.flush();
        long count = titleRepo.count();
        titleRepo.deleteAll();
        return ok(count);
    }

    @DeleteMapping("/roles")
    @Transactional
    public ResponseEntity<?> clearRoles() {
        em.createNativeQuery("UPDATE kpis SET owner_role_id = NULL").executeUpdate();
        em.createNativeQuery("UPDATE processes SET owner_role_id = NULL").executeUpdate();
        em.createNativeQuery("DELETE FROM process_roles").executeUpdate();
        em.createNativeQuery("DELETE FROM position_roles").executeUpdate();
        em.flush();
        long count = roleRepo.count();
        roleRepo.deleteAll();
        return ok(count);
    }

    @DeleteMapping("/positions")
    @Transactional
    public ResponseEntity<?> clearPositions() {
        em.createNativeQuery("DELETE FROM position_roles").executeUpdate();
        em.createNativeQuery("DELETE FROM unit_positions").executeUpdate();
        em.createNativeQuery("UPDATE user_profiles SET position_id = NULL").executeUpdate();
        em.flush();
        long count = positionRepo.count();
        positionRepo.deleteAll();
        return ok(count);
    }

    @DeleteMapping("/units")
    @Transactional
    public ResponseEntity<?> clearUnits() {
        em.createNativeQuery("DELETE FROM unit_positions").executeUpdate();
        em.createNativeQuery("UPDATE org_units SET parent_id = NULL").executeUpdate();
        em.createNativeQuery("UPDATE user_profiles SET org_unit_id = NULL").executeUpdate();
        em.flush();
        long count = unitRepo.count();
        unitRepo.deleteAll();
        return ok(count);
    }

    @DeleteMapping("/processes")
    @Transactional
    public ResponseEntity<?> clearProcesses() {
        em.createNativeQuery("DELETE FROM process_roles").executeUpdate();
        em.createNativeQuery("DELETE FROM kpis").executeUpdate();
        em.createNativeQuery("DELETE FROM process_ios").executeUpdate();
        em.createNativeQuery("DELETE FROM process_steps").executeUpdate();
        em.createNativeQuery("DELETE FROM process_risks").executeUpdate();
        em.createNativeQuery("DELETE FROM process_parties").executeUpdate();
        em.createNativeQuery("UPDATE processes SET parent_id = NULL").executeUpdate();
        em.flush();
        long count = processRepo.count();
        processRepo.deleteAll();
        return ok(count);
    }

    @DeleteMapping("/doc-types")
    @Transactional
    public ResponseEntity<?> clearDocTypes() {
        em.createNativeQuery("UPDATE documents SET doc_type_id = NULL").executeUpdate();
        em.flush();
        long count = docTypeRepo.count();
        docTypeRepo.deleteAll();
        return ok(count);
    }

    @DeleteMapping("/doc-statuses")
    @Transactional
    public ResponseEntity<?> clearDocStatuses() {
        em.createNativeQuery("UPDATE documents SET status_id = NULL").executeUpdate();
        em.flush();
        long count = docStatusRepo.count();
        docStatusRepo.deleteAll();
        return ok(count);
    }

    @DeleteMapping("/documents")
    @Transactional
    public ResponseEntity<?> clearDocuments() {
        em.createNativeQuery("DELETE FROM distributions").executeUpdate();
        em.createNativeQuery("DELETE FROM document_revisions").executeUpdate();
        em.createNativeQuery("UPDATE documents SET parent_id = NULL").executeUpdate();
        em.flush();
        long count = documentRepo.count();
        documentRepo.deleteAll();
        return ok(count);
    }

    private ResponseEntity<?> ok(long count) {
        return ResponseEntity.ok(Map.of("deleted", count));
    }
}
