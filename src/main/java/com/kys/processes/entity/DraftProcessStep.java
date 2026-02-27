package com.kys.processes.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "draft_process_steps")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DraftProcessStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_id", nullable = false)
    private Process process;

    @Column(name = "step_no")
    private int stepNo;

    @Column(name = "step_name")
    private String stepName;

    @Column(name = "trigger")
    private String trigger;

    @Column(name = "input")
    private String input;

    @Column(name = "work_done")
    private String workDone;

    @Column(name = "output")
    private String output;

    @Column(name = "transferred_role")
    private String transferredRole;

    @Column(name = "responsible")
    private String responsible;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
