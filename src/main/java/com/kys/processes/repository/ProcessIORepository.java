package com.kys.processes.repository;

import com.kys.processes.entity.ProcessIO;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProcessIORepository extends JpaRepository<ProcessIO, Long> {
    List<ProcessIO> findByProcessId(Long processId);
}
