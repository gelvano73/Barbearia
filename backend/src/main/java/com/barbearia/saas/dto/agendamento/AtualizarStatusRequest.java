package com.barbearia.saas.dto.agendamento;

import com.barbearia.saas.domain.enums.StatusAgendamento;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** DTO de entrada para alterar o status de um agendamento. */
@Data
public class AtualizarStatusRequest {

    @NotNull
    private StatusAgendamento status;
}
