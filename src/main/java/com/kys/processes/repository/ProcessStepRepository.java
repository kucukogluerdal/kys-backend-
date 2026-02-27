package com.kys.processes.repository;

import com.kys.processes.entity.ProcessStep;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProcessStepRepository extends JpaRepository<ProcessStep, Long> {
    List<ProcessStep> findByProcessIdOrderByStepNoAsc(Long processId);
}
