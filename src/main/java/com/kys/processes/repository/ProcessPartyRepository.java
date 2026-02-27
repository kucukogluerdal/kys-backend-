package com.kys.processes.repository;

import com.kys.processes.entity.ProcessParty;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProcessPartyRepository extends JpaRepository<ProcessParty, Long> {
    List<ProcessParty> findByProcessId(Long processId);
}
