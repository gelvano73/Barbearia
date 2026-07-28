package com.barbearia.saas.dto.recepcao;

import com.barbearia.saas.domain.enums.StatusFila;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** DTO de entrada para atualizar status na fila. */
@Data
public class AtualizarFilaStatusRequest {
    @NotNull
    private StatusFila status;
}
