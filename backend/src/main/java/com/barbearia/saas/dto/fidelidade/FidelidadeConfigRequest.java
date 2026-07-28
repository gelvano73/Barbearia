package com.barbearia.saas.dto.fidelidade;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** DTO de entrada da configuração do programa de fidelidade. */
@Data
public class FidelidadeConfigRequest {

    @NotNull
    @Min(1)
    private Integer pontosPorAtendimento = 1;

    @NotNull
    @Min(1)
    private Integer pontosParaResgate = 10;

    @NotBlank
    @Size(max = 255)
    private String descricao = "A cada 10 cortes = 1 grátis";

    @NotNull
    private Boolean ativo = true;
}
