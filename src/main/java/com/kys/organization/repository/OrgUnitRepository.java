package com.kys.organization.repository;

import com.kys.organization.entity.OrgUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrgUnitRepository extends JpaRepository<OrgUnit, Long> {
    List<OrgUnit> findByParentIsNull();
    List<OrgUnit> findByParentId(Long parentId);
    boolean existsByCode(String code);
    java.util.Optional<OrgUnit> findByCode(String code);
    List<OrgUnit> findByPositionsId(Long positionId);
}
