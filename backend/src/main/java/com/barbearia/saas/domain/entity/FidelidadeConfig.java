package com.barbearia.saas.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** Entidade JPA de configuração do programa de fidelidade da barbearia. */
@Entity
@Table(name = "fidelidade_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FidelidadeConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbearia_id", nullable = false, unique = true)
    private Barbearia barbearia;

    @Column(name = "pontos_por_atendimento", nullable = false)
    @Builder.Default
    private Integer pontosPorAtendimento = 1;

    @Column(name = "pontos_para_resgate", nullable = false)
    @Builder.Default
    private Integer pontosParaResgate = 10;

    @Column(nullable = false, length = 255)
    @Builder.Default
    private String descricao = "A cada 10 cortes = 1 grátis";

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
