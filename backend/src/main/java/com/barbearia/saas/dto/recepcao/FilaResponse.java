package com.barbearia.saas.dto.recepcao;

import com.barbearia.saas.domain.enums.StatusFila;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** DTO de saída de um item da fila de atendimento. */
@Data
@Builder
public class FilaResponse {
    private Long id;
    private Long clienteId;
    private String clienteNome;
    private Long barbeiroId;
    private String barbeiroNome;
    private Long servicoId;
    private String servicoNome;
    private Integer posicao;
    private StatusFila status;
    private Boolean prioridade;
    private String observacoes;
    private LocalDateTime criadoEm;
}
