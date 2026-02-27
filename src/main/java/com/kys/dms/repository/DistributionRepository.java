package com.kys.dms.repository;

import com.kys.dms.entity.Distribution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DistributionRepository extends JpaRepository<Distribution, Long> {
    List<Distribution> findByDocumentId(Long documentId);
}
