package com.barbearia.saas.service;

import com.barbearia.saas.config.EmailProperties;
import com.barbearia.saas.util.EmailUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.time.Instant;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Valida se o domínio do e-mail existe na internet (registros MX ou A/AAAA),
 * conforme boa prática para aceitar apenas e-mails reais no cadastro.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailDominioService {

    private final EmailProperties properties;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /** === Validação pública === */

    public boolean dominioExiste(String email) {
        if (!properties.isValidarDns()) {
            return true;
        }
        String dominio = EmailUtil.dominio(email);
        if (dominio == null) {
            return false;
        }

        CacheEntry cached = cache.get(dominio);
        if (cached != null && !cached.expirado()) {
            return cached.ok;
        }

        boolean ok = consultarDns(dominio);
        long ttl = properties.getDnsCacheMinutos() * 60_000L;
        cache.put(dominio, new CacheEntry(ok, Instant.now().toEpochMilli() + ttl));
        return ok;
    }

    public void validarOuFalhar(String email) {
        String normalizado = EmailUtil.normalizar(email);
        if (!EmailUtil.isValidoParaCadastro(normalizado)) {
            throw new com.barbearia.saas.exception.NegocioException(
                    "E-mail inválido. Use um endereço real (não temporário nem de exemplo).");
        }
        if (!dominioExiste(normalizado)) {
            throw new com.barbearia.saas.exception.NegocioException(
                    "Domínio do e-mail não encontrado no DNS. Informe um e-mail real e ativo.");
        }
    }

    /** === DNS === */

    private boolean consultarDns(String dominio) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("com.sun.jndi.dns.timeout.initial", String.valueOf(Math.max(500, properties.getDnsTimeoutMs())));
        env.put("com.sun.jndi.dns.timeout.retries", "1");

        try {
            InitialDirContext ctx = new InitialDirContext(env);
            try {
                if (temRegistro(ctx, dominio, "MX") || temRegistro(ctx, dominio, "A") || temRegistro(ctx, dominio, "AAAA")) {
                    return true;
                }
            } finally {
                ctx.close();
            }
        } catch (NamingException e) {
            log.debug("DNS falhou para {}: {}", dominio, e.getMessage());
        }
        return false;
    }

    private boolean temRegistro(InitialDirContext ctx, String dominio, String tipo) throws NamingException {
        Attributes attrs = ctx.getAttributes(dominio, new String[]{tipo});
        Attribute attr = attrs.get(tipo);
        return attr != null && attr.size() > 0;
    }

    private record CacheEntry(boolean ok, long expiraEm) {
        boolean expirado() {
            return Instant.now().toEpochMilli() > expiraEm;
        }
    }
}
