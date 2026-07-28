package com.barbearia.saas.dto.portalbarbeiro;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

/** DTO de horário de trabalho do barbeiro. */
@Data
@Builder
public class HorarioResponse {
    private Long id;
    private Integer diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFim;
    private Boolean ativo;
}
