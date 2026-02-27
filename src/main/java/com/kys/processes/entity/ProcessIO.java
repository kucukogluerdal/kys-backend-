package com.kys.processes.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "process_ios")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProcessIO {

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
    private IOType ioType;

    private int sortOrder;

    public enum IOType { INPUT, OUTPUT }
}
