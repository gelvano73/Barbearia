package com.barbearia.saas.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Entidade JPA de metas de desempenho (ex.: faturamento) do barbeiro. */
@Entity
@Table(name = "barbeiro_metas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarbeiroMeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbeiro_id", nullable = false)
    private Barbeiro barbeiro;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false)
    private Integer mes;

    @Column(name = "meta_atendimentos", nullable = false)
    @Builder.Default
    private Integer metaAtendimentos = 0;

    @Column(name = "meta_comissao", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal metaComissao = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
