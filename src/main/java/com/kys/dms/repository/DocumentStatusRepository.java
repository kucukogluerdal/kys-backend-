package com.kys.dms.repository;

import com.kys.dms.entity.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DocumentStatusRepository extends JpaRepository<DocumentStatus, Long> {
    Optional<DocumentStatus> findByCode(String code);
}
