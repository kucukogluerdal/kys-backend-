package com.kys.organization.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "titles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Title {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String code;

    @Builder.Default
    private boolean isActive = true;
}
