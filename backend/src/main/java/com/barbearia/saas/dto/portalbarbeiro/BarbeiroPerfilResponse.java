package com.barbearia.saas.dto.portalbarbeiro;

import lombok.Builder;
import lombok.Data;

/** DTO do perfil do barbeiro no portal. */
@Data
@Builder
public class BarbeiroPerfilResponse {
    private Long id;
    private String nome;
    private String telefone;
    private String especialidade;
    private String fotoUrl;
}
