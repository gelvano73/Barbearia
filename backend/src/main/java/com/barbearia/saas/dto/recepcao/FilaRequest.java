package com.barbearia.saas.dto.recepcao;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** DTO de entrada para incluir cliente na fila. */
@Data
public class FilaRequest {

    @NotNull
    private Long clienteId;

    private Long barbeiroId;
    private Long servicoId;
    private Boolean prioridade = false;

    @Size(max = 500)
    private String observacoes;
}
