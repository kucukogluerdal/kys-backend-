package com.kys.dms.entity;

import com.kys.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_revisions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DocumentRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    private String filePath;

    private String revisionNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revised_by_id")
    private User revisedBy;

    @CreationTimestamp
    private LocalDateTime revisedAt;
}
