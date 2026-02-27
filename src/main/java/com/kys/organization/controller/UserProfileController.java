package com.kys.organization.controller;

import com.kys.auth.UserRepository;
import com.kys.auth.entity.User;
import com.kys.organization.entity.UserProfile;
import com.kys.organization.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/organization/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileRepository profileRepo;
    private final UserRepository userRepo;
    private final TitleRepository titleRepo;
    private final PositionRepository positionRepo;
    private final OrgUnitRepository unitRepo;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    @Transactional
    public List<Map<String, Object>> list() {
        return profileRepo.findAll().stream().map(this::toMap).toList();
    }

    @GetMapping("/{id}")
    @Transactional
    public ResponseEntity<?> get(@PathVariable Long id) {
        return profileRepo.findById(id)
            .map(p -> ResponseEntity.ok(toMap(p)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        UserProfile p = new UserProfile();
        applyBody(p, body);
        return ResponseEntity.ok(toMap(profileRepo.save(p)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return profileRepo.findById(id).map(p -> {
            applyBody(p, body);
            return ResponseEntity.ok(toMap(profileRepo.save(p)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/access")
    public ResponseEntity<?> updateAccess(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return profileRepo.findById(id).map(profile -> {
            User user = profile.getUser();
            if (user == null) {
                user = new User();
            }
            if (body.get("username") != null && !body.get("username").toString().isBlank())
                user.setUsername(body.get("username").toString());
            if (body.get("password") != null && !body.get("password").toString().isBlank())
                user.setPassword(passwordEncoder.encode(body.get("password").toString()));
            if (body.get("role") != null) {
                try { user.setRole(User.Role.valueOf(body.get("role").toString())); }
                catch (Exception ignored) {}
            }
            if (body.get("isActive") != null) user.setActive(Boolean.TRUE.equals(body.get("isActive")));
            User saved = userRepo.save(user);
            profile.setUser(saved);
            profileRepo.save(profile);
            return ResponseEntity.ok(Map.<String, Object>of(
                "userId",   saved.getId(),
                "username", saved.getUsername(),
                "role",     saved.getRole().name(),
                "isActive", saved.isActive()
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!profileRepo.existsById(id)) return ResponseEntity.notFound().build();
        profileRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void applyBody(UserProfile p, Map<String, Object> body) {
        if (body.get("firstName") != null) p.setFirstName((String) body.get("firstName"));
        if (body.get("lastName") != null)  p.setLastName((String) body.get("lastName"));
        // fullName: önce firstName+lastName, yoksa doğrudan fullName alanından al
        String fn = p.getFirstName() != null ? p.getFirstName() : "";
        String ln = p.getLastName()  != null ? p.getLastName()  : "";
        if (!fn.isBlank() || !ln.isBlank()) {
            p.setFullName((fn + " " + ln).trim());
        } else if (body.get("fullName") != null) {
            p.setFullName((String) body.get("fullName"));
        }
        if (body.get("employeeId") != null) p.setEmployeeId((String) body.get("employeeId"));
        if (body.get("phone")      != null) p.setPhone((String) body.get("phone"));
        if (body.get("email")      != null) p.setEmail((String) body.get("email"));
        if (body.get("hireDate")   != null) {
            try { p.setHireDate(LocalDate.parse(body.get("hireDate").toString())); }
            catch (Exception ignored) {}
        }
        if (body.get("isActive") != null) p.setActive(Boolean.TRUE.equals(body.get("isActive")));
        if (body.get("userId") != null)
            userRepo.findById(Long.valueOf(body.get("userId").toString())).ifPresent(p::setUser);
        if (body.containsKey("titleId")) {
            if (body.get("titleId") == null || body.get("titleId").toString().isBlank()) p.setTitle(null);
            else titleRepo.findById(Long.valueOf(body.get("titleId").toString())).ifPresent(p::setTitle);
        }
        if (body.containsKey("positionId")) {
            if (body.get("positionId") == null || body.get("positionId").toString().isBlank()) p.setPosition(null);
            else positionRepo.findById(Long.valueOf(body.get("positionId").toString())).ifPresent(p::setPosition);
        }
        if (body.containsKey("orgUnitId")) {
            if (body.get("orgUnitId") == null || body.get("orgUnitId").toString().isBlank()) p.setOrgUnit(null);
            else unitRepo.findById(Long.valueOf(body.get("orgUnitId").toString())).ifPresent(p::setOrgUnit);
        }
    }

    private Map<String, Object> toMap(UserProfile p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("fullName",    p.getFullName() != null ? p.getFullName() : "");
        map.put("firstName",   p.getFirstName() != null ? p.getFirstName() : "");
        map.put("lastName",    p.getLastName() != null ? p.getLastName() : "");
        map.put("employeeId",  p.getEmployeeId() != null ? p.getEmployeeId() : "");
        map.put("phone",       p.getPhone() != null ? p.getPhone() : "");
        map.put("email",       p.getEmail() != null ? p.getEmail() : "");
        map.put("hireDate",    p.getHireDate() != null ? p.getHireDate().toString() : "");
        map.put("isActive", p.isActive());
        map.put("userId", p.getUser() != null ? p.getUser().getId() : null);
        map.put("username", p.getUser() != null ? p.getUser().getUsername() : "");
        map.put("titleId", p.getTitle() != null ? p.getTitle().getId() : null);
        map.put("titleName", p.getTitle() != null ? p.getTitle().getName() : "");
        map.put("positionId", p.getPosition() != null ? p.getPosition().getId() : null);
        map.put("positionName", p.getPosition() != null ? p.getPosition().getName() : "");
        map.put("positionLevel", p.getPosition() != null && p.getPosition().getLevel() != null
            ? p.getPosition().getLevel().name() : "");
        map.put("orgUnitId", p.getOrgUnit() != null ? p.getOrgUnit().getId() : null);
        map.put("orgUnitName", p.getOrgUnit() != null ? p.getOrgUnit().getName() : "");
        List<Map<String, Object>> roles = p.getPosition() != null
            ? p.getPosition().getRoles().stream()
                .map(r -> Map.<String, Object>of("id", r.getId(), "name", r.getName(), "roleType",
                    r.getRoleType() != null ? r.getRoleType().name() : ""))
                .toList()
            : List.of();
        map.put("roles", roles);
        return map;
    }
}
