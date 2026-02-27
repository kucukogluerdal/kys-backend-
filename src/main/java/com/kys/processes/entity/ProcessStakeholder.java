package com.kys.processes.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "process_stakeholders")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProcessStakeholder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_id", nullable = false)
    private Process process;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StakeholderType stakeholderType;

    public enum StakeholderType { INTERNAL, EXTERNAL }
}
