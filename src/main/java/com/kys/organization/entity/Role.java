package com.kys.organization.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    private RoleType roleType;

    private String description;

    @Builder.Default
    private boolean isActive = true;

    public enum RoleType {
        STRATEGIC, MANAGERIAL, OPERATIONAL
    }
}
