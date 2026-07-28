package com.barbearia.saas.dto.portal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** DTO de entrada para o cliente avaliar um atendimento. */
@Data
public class AvaliacaoRequest {

    @NotNull
    private Long agendamentoId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer nota;

    @Size(max = 500)
    private String comentario;
}
