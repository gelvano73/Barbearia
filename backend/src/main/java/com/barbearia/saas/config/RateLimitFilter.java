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
 * Filtro simples de rate limiting em memória (60 requisições/minuto por IP) aplicado às rotas
 * de autenticação, para mitigar força bruta em login/registro.
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int LIMITE_POR_JANELA = 60;
    private static final long JANELA_MS = 60_000L;

    private final Map<String, Contador> contadores = new ConcurrentHashMap<>();

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

        String ip = resolverIp(request);
        if (excedeuLimite(ip)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"mensagem\":\"Muitas requisições. Tente novamente em instantes.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean excedeuLimite(String ip) {
        long agora = Instant.now().toEpochMilli();
        Contador contador = contadores.computeIfAbsent(ip, k -> new Contador(agora));

        synchronized (contador) {
            if (agora - contador.inicioJanela > JANELA_MS) {
                contador.inicioJanela = agora;
                contador.total.set(0);
            }
            return contador.total.incrementAndGet() > LIMITE_POR_JANELA;
        }
    }

    private String resolverIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Contador {
        private volatile long inicioJanela;
        private final AtomicInteger total = new AtomicInteger(0);

        private Contador(long inicioJanela) {
            this.inicioJanela = inicioJanela;
        }
    }
}
