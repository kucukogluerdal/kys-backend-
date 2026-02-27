package com.kys.processes.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "process_steps")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProcessStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_id", nullable = false)
    private Process process;

    private int stepNo;

    @Column(nullable = false)
    private String name;

    private String description;
}
