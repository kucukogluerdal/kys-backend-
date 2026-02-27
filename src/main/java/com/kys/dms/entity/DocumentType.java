package com.kys.dms.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "document_types")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DocumentType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private int level;

    private String description;

    @Builder.Default
    private boolean isActive = true;
}
