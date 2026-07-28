package com.barbearia.saas.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** Entidade JPA do saldo atual de pontos de fidelidade do cliente. */
@Entity
@Table(name = "fidelidade_saldos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FidelidadeSaldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbearia_id", nullable = false)
    private Barbearia barbearia;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    private Cliente cliente;

    @Column(nullable = false)
    @Builder.Default
    private Integer pontos = 0;

    @Column(name = "pontos_acumulados", nullable = false)
    @Builder.Default
    private Integer pontosAcumulados = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer resgates = 0;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
