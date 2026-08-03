package com.barbearia.saas.dto.auth;

import com.barbearia.saas.domain.enums.PlanoAssinatura;
import com.barbearia.saas.domain.enums.Role;
import lombok.Builder;
import lombok.Data;

/** DTO de resposta de autenticação contendo token JWT e dados do usuário. */
@Data
@Builder
public class AuthResponse {
    private String token;
    private String tipo;
    private Long usuarioId;
    private Long clienteId;
    private Long barbeiroId;
    private String nome;
    private String email;
    private Role role;
    private Long barbeariaId;
    private String nomeBarbearia;
    private PlanoAssinatura plano;
}
