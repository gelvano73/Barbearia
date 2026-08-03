package com.barbearia.saas.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting em rotas de autenticação.
 * Login/OTP: 10 req/min por IP. Demais /api/auth: 30 req/min.
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int LIMITE_LOGIN = 10;
    private static final int LIMITE_AUTH = 30;
    private static final long JANELA_MS = 60_000L;

    private final Map<String, Contador> contadores = new ConcurrentHashMap<>();

    /** === Filtro === */

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (!path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        int limite = isLoginSensivel(path) ? LIMITE_LOGIN : LIMITE_AUTH;
        String ip = resolverIp(request);
        if (excedeuLimite(ip + "|" + (isLoginSensivel(path) ? "login" : "auth"), limite)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"mensagem\":\"Muitas tentativas. Aguarde um minuto e tente novamente.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** === Contadores === */

    private boolean isLoginSensivel(String path) {
        return path.contains("/login") || path.contains("/otp/") || path.contains("/registro")
                || path.contains("/recuperar-senha") || path.contains("/redefinir-senha");
    }

    private boolean excedeuLimite(String chave, int limite) {
        long agora = Instant.now().toEpochMilli();
        Contador contador = contadores.computeIfAbsent(chave, k -> new Contador(agora));

        synchronized (contador) {
            if (agora - contador.inicioJanela > JANELA_MS) {
                contador.inicioJanela = agora;
                contador.total.set(0);
            }
            return contador.total.incrementAndGet() > limite;
        }
    }

    private String resolverIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private static final class Contador {
        private volatile long inicioJanela;
        private final AtomicInteger total = new AtomicInteger(0);

        private Contador(long inicioJanela) {
            this.inicioJanela = inicioJanela;
        }
    }
}
