package com.barbearia.saas.dto.portal;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** DTO de saída de uma avaliação. */
@Data
@Builder
public class AvaliacaoResponse {
    private Long id;
    private Long agendamentoId;
    private Long barbeiroId;
    private String barbeiroNome;
    private Integer nota;
    private String comentario;
    private LocalDateTime criadoEm;
}
