package com.kys.organization.repository;

import com.kys.organization.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    List<UserProfile> findByIsActiveTrue();
    Optional<UserProfile> findByUserId(Long userId);
    Optional<UserProfile> findByEmployeeId(String employeeId);
    boolean existsByEmployeeId(String employeeId);
}
