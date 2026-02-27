package com.kys.processes.repository;

import com.kys.processes.entity.DraftProcessStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DraftProcessStepRepository extends JpaRepository<DraftProcessStep, Long> {
    List<DraftProcessStep> findByProcessIdOrderByStepNoAsc(Long processId);

    @Transactional
    void deleteByProcessId(Long processId);
}
