package com.kys.processes.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Süreç kodu yapı tanımı — tek kayıt (singleton config).
 *
 * Örnek:
 *   separator        = "-"
 *   fieldNames       = ["Org","Unit","Type","MainNo","SubNo"]
 *   levelRulesJson   = [
 *     {"count":4,"index":2,"value":"AS","level":"L1"},
 *     {"count":4,"index":2,"value":"SR","level":"L2"},
 *     {"count":5,"index":-1,"value":null,"level":"L3"}
 *   ]
 *   parentTemplateL2 = "{Org}-{Unit}-AS-{MainNo}"
 *   parentTemplateL3 = "{Org}-{Unit}-SR-{MainNo}"
 */
@Entity
@Table(name = "process_code_configs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProcessCodeConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ayırıcı karakter(ler), örn: "-" veya "-." */
    @Column(length = 10)
    private String separator;

    /** Alan isimleri JSON dizisi: ["Org","Unit","Type","MainNo","SubNo"] */
    @Column(columnDefinition = "TEXT")
    private String fieldNamesJson;

    /**
     * Seviye kuralları JSON dizisi. Her kural:
     *   count: kaç parça olmalı (-1 = farketmez)
     *   index: hangi index'teki parçaya bakılacak (-1 = bakılmaz)
     *   value: beklenen değer (null = bakılmaz)
     *   level: "L1" | "L2" | "L3"
     * Kurallar sırayla uygulanır, ilk eşleşen kazanır.
     */
    @Column(columnDefinition = "TEXT")
    private String levelRulesJson;

    /** L2 süreci için parent kodu şablonu, ör: "{Org}-{Unit}-AS-{MainNo}" */
    @Column(length = 500)
    private String parentTemplateL2;

    /** L3 süreci için parent kodu şablonu, ör: "{Org}-{Unit}-SR-{MainNo}" */
    @Column(length = 500)
    private String parentTemplateL3;
}
