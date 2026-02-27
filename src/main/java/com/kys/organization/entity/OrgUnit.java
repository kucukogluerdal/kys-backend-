package com.kys.organization.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "org_units")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OrgUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private OrgUnit parent;

    @Builder.Default
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<OrgUnit> children = new ArrayList<>();

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "unit_positions",
        joinColumns = @JoinColumn(name = "unit_id"),
        inverseJoinColumns = @JoinColumn(name = "position_id")
    )
    private List<Position> positions = new ArrayList<>();

    @Builder.Default
    private boolean isActive = true;
}
