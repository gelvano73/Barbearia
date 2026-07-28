package com.barbearia.saas.dto.portalbarbeiro;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** DTO de entrada para solicitar férias. */
@Data
public class FeriasRequest {

    @NotNull
    private LocalDate dataInicio;

    @NotNull
    private LocalDate dataFim;

    @Size(max = 255)
    private String motivo;
}
