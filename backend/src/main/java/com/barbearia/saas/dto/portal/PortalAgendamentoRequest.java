package com.barbearia.saas.dto.portal;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/** DTO de entrada para o cliente criar agendamento no portal. */
@Data
public class PortalAgendamentoRequest {

    @NotNull
    private Long barbeiroId;

    private Long servicoId;

    @NotNull
    @Future
    private LocalDateTime dataHora;

    @Size(max = 500)
    private String observacoes;
}
