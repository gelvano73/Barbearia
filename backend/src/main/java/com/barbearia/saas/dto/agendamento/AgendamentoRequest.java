package com.barbearia.saas.dto.agendamento;

import com.barbearia.saas.domain.enums.StatusAgendamento;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/** DTO de entrada para criar ou atualizar um agendamento. */
@Data
public class AgendamentoRequest {

    @NotNull
    private Long clienteId;

    @NotNull
    private Long barbeiroId;

    private Long servicoId;

    @NotNull
    @Future
    private LocalDateTime dataHora;

    @Min(15)
    private Integer duracaoMinutos = 30;

    @Size(max = 150)
    private String servico;

    @Size(max = 500)
    private String observacoes;

    private StatusAgendamento status;
}
