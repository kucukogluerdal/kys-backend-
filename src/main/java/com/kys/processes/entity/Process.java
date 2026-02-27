package com.kys.processes.entity;

import com.kys.organization.entity.Position;
import com.kys.organization.entity.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "processes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Process {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Process parent;

    @Enumerated(EnumType.STRING)
    private Level level;

    @Enumerated(EnumType.STRING)
    private ProcessType processType;

    @Enumerated(EnumType.STRING)
    private Criticality criticality;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.ACTIVE;

    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String purpose;

    private String strategicGoal;

    private String strategicTarget;

    private String startPoint;

    private String endPoint;

    @Column(columnDefinition = "TEXT")
    private String processScope;

    @Column(columnDefinition = "TEXT")
    private String affectedBy;

    @Column(columnDefinition = "TEXT")
    private String affects;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_role_id")
    private Role ownerRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_position_id")
    private Position ownerPosition;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "process_roles",
        joinColumns = @JoinColumn(name = "process_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private List<Role> ownerRoles = new ArrayList<>();

    @Builder.Default
    private boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "process", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProcessIO> ios = new ArrayList<>();

    @OneToMany(mappedBy = "process", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProcessStep> steps = new ArrayList<>();

    @OneToMany(mappedBy = "process", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProcessRisk> risks = new ArrayList<>();

    @OneToMany(mappedBy = "process", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProcessParty> parties = new ArrayList<>();

    public enum Level { L1, L2, L3 }
    public enum ProcessType { CORE, SUPPORT, MANAGEMENT }
    public enum Criticality { HIGH, MEDIUM, LOW }
    public enum Status { ACTIVE, PASSIVE }
}
