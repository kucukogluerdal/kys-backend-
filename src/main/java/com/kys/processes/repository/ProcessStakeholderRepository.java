package com.kys.processes.repository;

import com.kys.processes.entity.ProcessStakeholder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcessStakeholderRepository extends JpaRepository<ProcessStakeholder, Long> {
    List<ProcessStakeholder> findByProcessId(Long processId);
}
