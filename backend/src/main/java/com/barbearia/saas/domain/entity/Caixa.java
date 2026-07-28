package com.barbearia.saas.domain.entity;

import com.barbearia.saas.domain.enums.StatusCaixa;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Entidade JPA que representa o caixa diário (aberto/fechado) da unidade. */
@Entity
@Table(name = "caixas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Caixa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbearia_id", nullable = false)
    private Barbearia barbearia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @CreationTimestamp
    @Column(name = "aberto_em", nullable = false, updatable = false)
    private LocalDateTime abertoEm;

    @Column(name = "fechado_em")
    private LocalDateTime fechadoEm;

    @Column(name = "valor_abertura", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal valorAbertura = BigDecimal.ZERO;

    @Column(name = "valor_informado_fechamento", precision = 12, scale = 2)
    private BigDecimal valorInformadoFechamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusCaixa status = StatusCaixa.ABERTO;

    @Column(length = 500)
    private String observacoes;
}
