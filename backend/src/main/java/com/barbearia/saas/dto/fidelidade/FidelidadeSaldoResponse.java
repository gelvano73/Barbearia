package com.barbearia.saas.dto.fidelidade;

import lombok.Builder;
import lombok.Data;

/** DTO de saída com o saldo de pontos do cliente. */
@Data
@Builder
public class FidelidadeSaldoResponse {
    private Long clienteId;
    private String clienteNome;
    private Integer pontos;
    private Integer pontosAcumulados;
    private Integer resgates;
    private Integer pontosParaResgate;
    private Boolean podeResgatar;
}
