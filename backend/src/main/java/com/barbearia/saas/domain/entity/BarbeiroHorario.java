package com.barbearia.saas.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

/** Entidade JPA da grade de horários de trabalho do barbeiro. */
@Entity
@Table(name = "barbeiro_horarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarbeiroHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbeiro_id", nullable = false)
    private Barbeiro barbeiro;

    @Column(name = "dia_semana", nullable = false)
    private Integer diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
