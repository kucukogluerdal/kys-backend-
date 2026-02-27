package com.kys.organization.repository;

import com.kys.organization.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PositionRepository extends JpaRepository<Position, Long> {
    List<Position> findByIsActiveTrue();
    boolean existsByCode(String code);
    java.util.Optional<com.kys.organization.entity.Position> findByCode(String code);
}
