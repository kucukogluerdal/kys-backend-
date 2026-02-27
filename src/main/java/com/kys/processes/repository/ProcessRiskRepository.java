package com.kys.processes.repository;

import com.kys.processes.entity.ProcessRisk;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProcessRiskRepository extends JpaRepository<ProcessRisk, Long> {
    List<ProcessRisk> findByProcessId(Long processId);
}
