package com.barbearia.saas.dto.fidelidade;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** DTO de entrada para resgatar pontos de fidelidade. */
@Data
public class FidelidadeResgateRequest {

    @NotNull
    private Long clienteId;

    @Size(max = 255)
    private String observacao;
}
