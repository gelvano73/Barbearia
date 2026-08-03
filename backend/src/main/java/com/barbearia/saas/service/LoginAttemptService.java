package com.barbearia.saas.service;

import com.barbearia.saas.exception.NegocioException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controle de tentativas de login por chave (e-mail/CPF) para mitigar força bruta,
 * com bloqueio temporário após falhas consecutivas.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_FALHAS = 5;
    private static final long BLOQUEIO_MS = 15 * 60_000L;

    private final Map<String, Tentativa> tentativas = new ConcurrentHashMap<>();

    /** === Controle de tentativas === */

    public void verificarNaoBloqueado(String chave) {
        Tentativa t = tentativas.get(normalizar(chave));
        if (t == null) {
            return;
        }
        long agora = Instant.now().toEpochMilli();
        if (t.bloqueadoAte > agora) {
            long minutos = Math.max(1, (t.bloqueadoAte - agora + 59_999) / 60_000);
            throw new NegocioException("Conta temporariamente bloqueada por tentativas inválidas. Tente em "
                    + minutos + " minuto(s).");
        }
        if (t.bloqueadoAte > 0 && t.bloqueadoAte <= agora) {
            tentativas.remove(normalizar(chave));
        }
    }

    public void registrarFalha(String chave) {
        String k = normalizar(chave);
        long agora = Instant.now().toEpochMilli();
        Tentativa t = tentativas.computeIfAbsent(k, x -> new Tentativa());
        synchronized (t) {
            t.falhas++;
            if (t.falhas >= MAX_FALHAS) {
                t.bloqueadoAte = agora + BLOQUEIO_MS;
                t.falhas = 0;
            }
        }
    }

    public void registrarSucesso(String chave) {
        tentativas.remove(normalizar(chave));
    }

    /** === Auxiliares === */

    private String normalizar(String chave) {
        return chave == null ? "" : chave.trim().toLowerCase();
    }

    private static final class Tentativa {
        private int falhas;
        private long bloqueadoAte;
    }
}
