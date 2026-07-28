package com.barbearia.saas.dto.fidelidade;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** DTO do painel de fidelidade visto pelo cliente. */
@Data
@Builder
public class FidelidadeMeuPainelResponse {
    private FidelidadeConfigResponse config;
    private FidelidadeSaldoResponse saldo;
    private List<FidelidadeMovimentoResponse> historico;
}
