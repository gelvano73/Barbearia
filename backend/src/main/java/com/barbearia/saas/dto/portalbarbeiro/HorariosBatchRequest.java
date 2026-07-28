package com.barbearia.saas.dto.portalbarbeiro;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;
import java.util.List;

/** DTO de entrada para salvar a grade de horários em lote. */
@Data
public class HorariosBatchRequest {

    @NotEmpty
    @Valid
    private List<HorarioItem> horarios;

    @Data
    public static class HorarioItem {
        @NotNull
        @Min(0)
        @Max(6)
        private Integer diaSemana;

        @NotNull
        private LocalTime horaInicio;

        @NotNull
        private LocalTime horaFim;

        private Boolean ativo = true;
    }
}
