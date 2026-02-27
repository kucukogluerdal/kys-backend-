package com.kys.dms.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "document_statuses")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DocumentStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "sort_order")
    private int order;

    @Builder.Default
    private boolean isActive = true;
}
