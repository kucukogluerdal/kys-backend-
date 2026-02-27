package com.kys.processes.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "process_parties")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProcessParty {

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
    private PartyType partyType;

    public enum PartyType { SUPPLIER, CUSTOMER }
}
