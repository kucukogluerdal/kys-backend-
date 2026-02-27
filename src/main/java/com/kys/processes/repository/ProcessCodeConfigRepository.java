package com.kys.processes.repository;

import com.kys.processes.entity.ProcessCodeConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessCodeConfigRepository extends JpaRepository<ProcessCodeConfig, Long> {
    Optional<ProcessCodeConfig> findFirstByOrderByIdAsc();
}
