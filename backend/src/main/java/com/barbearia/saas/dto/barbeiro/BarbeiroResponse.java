package com.barbearia.saas.dto.barbeiro;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** DTO de saída com dados do barbeiro. */
@Data
@Builder
public class BarbeiroResponse {
    private Long id;
    private String nome;
    private String telefone;
    private String especialidade;
    private String fotoUrl;
    private Boolean ativo;
    private Long usuarioId;
    private LocalDateTime criadoEm;
}
