package com.barbearia.saas.dto.checkin;

import com.barbearia.saas.domain.enums.MetodoCheckin;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** DTO de saída com o resultado de um check-in. */
@Data
@Builder
public class CheckinResponse {
    private Long id;
    private Long clienteId;
    private String clienteNome;
    private MetodoCheckin metodo;
    private BigDecimal confianca;
    private String fotoUrl;
    private LocalDateTime criadoEm;
    private String mensagem;
}
