package com.barbearia.saas.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Entidade JPA de produto controlado no estoque da barbearia. */
@Entity
@Table(name = "produtos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbearia_id", nullable = false)
    private Barbearia barbearia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id")
    private Unidade unidadeLoja;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String unidade = "UN";

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal preco = BigDecimal.ZERO;

    @Column(name = "descricao_venda", length = 500)
    private String descricaoVenda;

    @Column(name = "marketplace_ativo", nullable = false)
    @Builder.Default
    private Boolean marketplaceAtivo = false;

    @Column(nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal quantidade = BigDecimal.ZERO;

    @Column(name = "estoque_minimo", nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal estoqueMinimo = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
