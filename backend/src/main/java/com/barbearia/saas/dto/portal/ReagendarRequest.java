package com.barbearia.saas.dto.portal;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/** DTO de entrada para reagendar um horário no portal. */
@Data
public class ReagendarRequest {

    @NotNull
    @Future
    private LocalDateTime dataHora;
}
