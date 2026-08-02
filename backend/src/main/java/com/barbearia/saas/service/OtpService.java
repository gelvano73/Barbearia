package com.barbearia.saas.service;

import com.barbearia.saas.exception.NegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Gera e valida tokens OTP enviados ao telefone cadastrado (WhatsApp/e-mail). */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final long EXPIRA_MS = 10 * 60_000L;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final NotificacaoService notificacaoService;
    private final Map<String, Codigo> codigos = new ConcurrentHashMap<>();

    public String gerarEEnviar(String chaveLogin, String telefone, Long barbeariaId, String nome) {
        if (telefone == null || telefone.isBlank()) {
            throw new NegocioException("Usuário sem telefone cadastrado para receber o código");
        }
        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        codigos.put(normalizar(chaveLogin), new Codigo(codigo, Instant.now().toEpochMilli() + EXPIRA_MS));

        String mensagem = "Olá" + (nome != null ? ", " + nome : "") + "! Seu código de acesso Barba SaaS é: "
                + codigo + ". Válido por 10 minutos.";
        // Reusa canal WhatsApp/e-mail via cliente sintético mínimo
        com.barbearia.saas.domain.entity.Cliente destino = com.barbearia.saas.domain.entity.Cliente.builder()
                .nome(nome != null ? nome : "Usuário")
                .telefone(telefone)
                .build();
        notificacaoService.notificarCliente(destino, barbeariaId != null ? barbeariaId : 0L,
                "Código de acesso", mensagem);
        log.info("[OTP] código gerado para login={} telefone={}", chaveLogin, telefone);
        return telefoneMascarado(telefone);
    }

    public void validar(String chaveLogin, String codigoInformado) {
        Codigo stored = codigos.get(normalizar(chaveLogin));
        if (stored == null || Instant.now().toEpochMilli() > stored.expiraEm) {
            throw new NegocioException("Código expirado ou inexistente. Solicite um novo.");
        }
        if (codigoInformado == null || !stored.codigo.equals(codigoInformado.trim())) {
            throw new NegocioException("Código inválido");
        }
        codigos.remove(normalizar(chaveLogin));
    }

    private String normalizar(String chave) {
        return chave == null ? "" : chave.trim().toLowerCase();
    }

    private String telefoneMascarado(String telefone) {
        String d = telefone.replaceAll("\\D", "");
        if (d.length() < 4) {
            return "****";
        }
        return "***" + d.substring(d.length() - 4);
    }

    private record Codigo(String codigo, long expiraEm) {
    }
}
