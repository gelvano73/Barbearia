package com.barbearia.saas.dto.portal;

import lombok.Builder;
import lombok.Data;

/** DTO de horário disponível para agendamento no portal. */
@Data
@Builder
public class HorarioDisponivelResponse {
    private String dataHora;
    private String label;
    private Long barbeiroId;
    private String barbeiroNome;
    private Long servicoId;
    private String servicoNome;
}
