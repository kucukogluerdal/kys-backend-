package com.kys.dms.repository;

import com.kys.dms.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByParentIsNull();
    Optional<Document> findByCode(String code);
}
