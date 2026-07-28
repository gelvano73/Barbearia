package com.barbearia.saas.domain.entity;

import com.barbearia.saas.domain.enums.TipoFidelidadeMovimento;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Entidade JPA de crédito/débito de pontos de fidelidade. */
@Entity
@Table(name = "fidelidade_movimentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FidelidadeMovimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbearia_id", nullable = false)
    private Barbearia barbearia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoFidelidadeMovimento tipo;

    @Column(nullable = false)
    private Integer pontos;

    @Column(name = "saldo_apos", nullable = false)
    private Integer saldoApos;

    @Column(length = 255)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id")
    private Agendamento agendamento;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
