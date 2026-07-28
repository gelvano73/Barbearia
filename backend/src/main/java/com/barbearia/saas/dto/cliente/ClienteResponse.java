package com.barbearia.saas.dto.cliente;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** DTO de saída com dados do cliente. */
@Data
@Builder
public class ClienteResponse {
    private Long id;
    private String nome;
    private String telefone;
    private String email;
    private String observacoes;
    private String fotoUrl;
    private Boolean ativo;
    private LocalDateTime criadoEm;
}
