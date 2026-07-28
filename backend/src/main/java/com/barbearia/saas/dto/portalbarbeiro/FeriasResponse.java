package com.barbearia.saas.dto.portalbarbeiro;

import com.barbearia.saas.domain.enums.StatusFerias;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** DTO de saída de solicitação de férias. */
@Data
@Builder
public class FeriasResponse {
    private Long id;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String motivo;
    private StatusFerias status;
    private LocalDateTime criadoEm;
}
