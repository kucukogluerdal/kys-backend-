package com.kys.processes.entity;

import com.kys.organization.entity.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "kpis")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class KPI {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_id", nullable = false)
    private Process process;

    @Column(nullable = false)
    private String name;

    private String definition;

    private String calculationMethod;

    @Enumerated(EnumType.STRING)
    private Frequency frequency;

    private String targetValue;

    private String thresholds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_role_id")
    private Role ownerRole;

    @Builder.Default
    private boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum Frequency { MONTHLY, QUARTERLY, YEARLY }
}
