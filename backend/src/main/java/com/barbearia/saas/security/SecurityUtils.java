package com.barbearia.saas.security;

import com.barbearia.saas.exception.NegocioException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Utilitários para obter o usuário autenticado e o tenant (barbearia) corrente. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** Obtém o usuário autenticado no contexto de segurança. */
    public static UsuarioPrincipal getUsuarioAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UsuarioPrincipal principal)) {
            throw new IllegalStateException("Usuário não autenticado");
        }
        return principal;
    }

    /** Obtém o ID da barbearia (tenant) do usuário autenticado. */
    public static Long getBarbeariaIdAtual() {
        return getUsuarioAtual().getBarbeariaId();
    }

    /** Obtém o ID do cliente associado ao usuário autenticado. */
    public static Long getClienteIdAtual() {
        Long clienteId = getUsuarioAtual().getClienteId();
        if (clienteId == null) {
            throw new NegocioException("Conta de cliente não vinculada");
        }
        return clienteId;
    }

    /** Obtém o ID do barbeiro associado ao usuário autenticado. */
    public static Long getBarbeiroIdAtual() {
        Long barbeiroId = getUsuarioAtual().getBarbeiroId();
        if (barbeiroId == null) {
            throw new NegocioException("Conta de barbeiro não vinculada");
        }
        return barbeiroId;
    }
}
