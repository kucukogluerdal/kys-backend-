package com.kys.organization.repository;

import com.kys.organization.entity.Title;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TitleRepository extends JpaRepository<Title, Long> {
    List<Title> findByIsActiveTrue();
    boolean existsByCode(String code);
    java.util.Optional<com.kys.organization.entity.Title> findByCode(String code);
}
