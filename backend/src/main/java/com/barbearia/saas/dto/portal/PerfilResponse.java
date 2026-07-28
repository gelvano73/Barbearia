package com.barbearia.saas.dto.portal;

import lombok.Builder;
import lombok.Data;

/** DTO de saída do perfil do cliente no portal. */
@Data
@Builder
public class PerfilResponse {
    private Long clienteId;
    private String nome;
    private String email;
    private String telefone;
    private String fotoUrl;
    private Long barbeariaId;
    private String nomeBarbearia;
}
