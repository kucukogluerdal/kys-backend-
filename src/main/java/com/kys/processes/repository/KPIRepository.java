package com.kys.processes.repository;

import com.kys.processes.entity.KPI;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KPIRepository extends JpaRepository<KPI, Long> {
    List<KPI> findByProcessId(Long processId);
    List<KPI> findByIsActiveTrue();
}
