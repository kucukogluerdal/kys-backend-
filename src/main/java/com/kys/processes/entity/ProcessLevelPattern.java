package com.kys.processes.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Süreç kodu segment → seviye eşlemesi.
 * Bu tablo üzerinden hem Excel import hem de otomatik ilişkilendirme çalışır.
 * Yeni bir kurulumda sadece bu tabloya kayıt eklenir, kod değişmez.
 *
 * Örnek:  segment=AS  baseLevel=L1  label=Ana Süreç
 *         segment=SR  baseLevel=L2  label=Alt Süreç
 *
 * Tespit kuralı:
 *   KADEM-VKF-AS-01      → AS=L1, 1 sayısal → L1
 *   KADEM-VKF-SR-01      → SR=L2, 1 sayısal → L2
 *   KADEM-VKF-SR-01-001  → SR=L2, 2 sayısal → L3
 *
 * İlişkilendirme kuralı:
 *   SR-01 → prefix öncesi (KADEM-VKF) + L1 segmenti (AS) → KADEM-VKF-AS-01 parent olur
 */
@Entity
@Table(name = "process_level_patterns")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProcessLevelPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String segment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Process.Level baseLevel;

    @Column(length = 100)
    private String label;
}
