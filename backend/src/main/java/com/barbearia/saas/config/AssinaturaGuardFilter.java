package com.barbearia.saas.config;

import com.barbearia.saas.domain.enums.StatusAssinatura;
import com.barbearia.saas.domain.repository.BarbeariaRepository;
import com.barbearia.saas.security.UsuarioPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Filtro que bloqueia o acesso à API quando a assinatura da barbearia está com status BLOQUEADA,
 * exceto para as rotas de autenticação, webhooks, endpoints públicos e da própria assinatura.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AssinaturaGuardFilter extends OncePerRequestFilter {

    private static final List<String> ROTAS_LIVRES = List.of(
            "/api/assinatura/**",
            "/api/auth/**",
            "/api/webhooks/**",
            "/api/public/**"
    );

    private final BarbeariaRepository barbeariaRepository;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (!path.startsWith("/api/") || rotaLivre(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UsuarioPrincipal principal
                && assinaturaBloqueada(principal.getBarbeariaId())) {
            writePaymentRequired(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean rotaLivre(String path) {
        return ROTAS_LIVRES.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean assinaturaBloqueada(Long barbeariaId) {
        if (barbeariaId == null) {
            return false;
        }
        try {
            return barbeariaRepository.findById(barbeariaId)
                    .map(b -> b.getAssinaturaStatus() == StatusAssinatura.BLOQUEADA)
                    .orElse(false);
        } catch (Exception e) {
            log.warn("Falha ao verificar assinatura da barbearia {}: {}", barbeariaId, e.getMessage());
            return false;
        }
    }

    private void writePaymentRequired(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.PAYMENT_REQUIRED.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"mensagem\":\"Assinatura bloqueada. Regularize o pagamento para continuar utilizando o sistema.\"}");
    }
}
