package com.barbearia.saas.config;

import com.barbearia.saas.security.SecurityUtils;
import com.barbearia.saas.security.UsuarioPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Auditoria leve de acessos autenticados (usuário, papel, barbearia, rota e status HTTP).
 * Em produção, encaminhe estes logs para o agregador (Sentry/Datadog/CloudWatch).
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
@Slf4j
public class AcessoAuditoriaFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long inicio = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            String path = request.getRequestURI();
            if (path != null && path.startsWith("/api/") && !path.startsWith("/api/auth/")) {
                try {
                    UsuarioPrincipal user = SecurityUtils.getUsuarioAtual();
                    log.info("AUDIT userId={} email={} role={} barbeariaId={} {} {} status={} {}ms",
                            user.getId(),
                            user.getUsername(),
                            user.getRole(),
                            user.getBarbeariaId(),
                            request.getMethod(),
                            path,
                            response.getStatus(),
                            System.currentTimeMillis() - inicio);
                } catch (Exception ignored) {
                    // rotas públicas ou sem principal — sem auditoria de usuário
                }
            }
        }
    }
}
