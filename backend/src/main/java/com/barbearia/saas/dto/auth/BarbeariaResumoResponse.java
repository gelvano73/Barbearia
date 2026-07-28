package com.barbearia.saas.dto.auth;

import lombok.Builder;
import lombok.Data;

/** DTO resumido da barbearia retornado no fluxo de autenticação. */
@Data
@Builder
public class BarbeariaResumoResponse {
    private Long id;
    private String nome;
}
