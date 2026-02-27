package com.kys.organization.repository;

import com.kys.organization.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    List<Role> findByIsActiveTrue();
    boolean existsByCode(String code);
    Optional<Role> findByCode(String code);
}
