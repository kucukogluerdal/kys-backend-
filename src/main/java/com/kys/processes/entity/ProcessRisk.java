package com.kys.processes.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "process_risks")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProcessRisk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_id", nullable = false)
    private Process process;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private RiskLevel probability;

    @Enumerated(EnumType.STRING)
    private RiskLevel impact;

    @Column(columnDefinition = "TEXT")
    private String mitigation;

    public enum RiskLevel { HIGH, MEDIUM, LOW }
}
