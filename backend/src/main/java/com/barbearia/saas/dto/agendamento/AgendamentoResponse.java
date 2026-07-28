package com.barbearia.saas.dto.agendamento;

import com.barbearia.saas.domain.enums.StatusAgendamento;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** DTO de saída com os dados de um agendamento. */
@Data
@Builder
public class AgendamentoResponse {
    private Long id;
    private Long clienteId;
    private String clienteNome;
    private Long barbeiroId;
    private String barbeiroNome;
    private Long servicoId;
    private LocalDateTime dataHora;
    private Integer duracaoMinutos;
    private StatusAgendamento status;
    private String servico;
    private String observacoes;
    private Boolean podeAvaliar;
    private LocalDateTime criadoEm;
}
