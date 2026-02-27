package com.kys.organization.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "positions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(50)")
    private Level level;

    private String description;

    @Builder.Default
    private boolean isActive = true;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "position_roles",
        joinColumns = @JoinColumn(name = "position_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<Role> roles = new ArrayList<>();

    public enum Level {
        GOVERNANCE, EXECUTIVE, MANAGER, STAFF
    }
}
