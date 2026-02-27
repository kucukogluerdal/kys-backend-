package com.kys.processes.repository;

import com.kys.processes.entity.Process;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProcessRepository extends JpaRepository<Process, Long> {
    List<Process> findByParentIsNull();
    List<Process> findByIsActiveTrue();
    Optional<Process> findByCode(String code);
}
