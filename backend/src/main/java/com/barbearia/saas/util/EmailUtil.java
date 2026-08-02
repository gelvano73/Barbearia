package com.barbearia.saas.util;

import java.net.IDN;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validação de e-mail real para cadastro:
 * formato (RFC 5321 simplificado), TLD válido, rejeição de domínios
 * de exemplo/descartáveis e opcionalmente verificação DNS (MX/A).
 */
public final class EmailUtil {

    /**
     * Local-part + domínio com TLD de 2+ letras (aceita IDN via punycode na checagem DNS).
     * Mais rigoroso que {@code @Email} do Bean Validation (que aceita vários casos inválidos).
     */
    private static final Pattern FORMATO = Pattern.compile(
            "^[a-zA-Z0-9](?:[a-zA-Z0-9._%+-]{0,62}[a-zA-Z0-9])?@"
                    + "[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?"
                    + "(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+$"
    );

    private static final Set<String> DOMINIOS_RESERVADOS = Set.of(
            "example.com", "example.org", "example.net",
            "test.com", "test.org", "localhost", "invalid",
            "email.com", "mailinator.local", "domain.com",
            "seuemail.com", "emailfalso.com", "ficticio.com"
    );

    /** Domínios descartáveis / temporários comuns (não aceitos em cadastro). */
    private static final Set<String> DOMINIOS_DESCARTAVEIS = Set.of(
            "mailinator.com", "guerrillamail.com", "guerrillamail.net", "sharklasers.com",
            "tempmail.com", "temp-mail.org", "temp-mail.io", "throwawaymail.com",
            "yopmail.com", "yopmail.fr", "trashmail.com", "discard.email",
            "10minutemail.com", "10minutemail.net", "minutemail.com",
            "fakeinbox.com", "getnada.com", "maildrop.cc", "mailnesia.com",
            "dispostable.com", "mozmail.com", "tempail.com", "emailondeck.com",
            "mailcatch.com", "mytemp.email", "tmpmail.org", "tmpmail.net",
            "moakt.com", "correo-temporal.com", "tempmailo.com", "burnermail.io"
    );

    private EmailUtil() {
    }

    public static String normalizar(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isFormatoValido(String email) {
        String e = normalizar(email);
        if (e.isBlank() || e.length() > 150 || e.contains("..") || e.startsWith(".") || e.contains("@.")) {
            return false;
        }
        if (!FORMATO.matcher(e).matches()) {
            return false;
        }
        String dominio = dominio(e);
        if (dominio == null || !dominio.contains(".")) {
            return false;
        }
        String tld = dominio.substring(dominio.lastIndexOf('.') + 1);
        return tld.length() >= 2 && tld.chars().allMatch(Character::isLetter);
    }

    public static String dominio(String email) {
        String e = normalizar(email);
        int at = e.lastIndexOf('@');
        if (at < 1 || at == e.length() - 1) {
            return null;
        }
        return e.substring(at + 1);
    }

    public static boolean isDominioReservadoOuDescartavel(String email) {
        String d = dominio(email);
        if (d == null) {
            return true;
        }
        String ascii;
        try {
            ascii = IDN.toASCII(d).toLowerCase(Locale.ROOT);
        } catch (Exception ex) {
            return true;
        }
        if (DOMINIOS_RESERVADOS.contains(ascii) || DOMINIOS_DESCARTAVEIS.contains(ascii)) {
            return true;
        }
        // subdomínio de descartável (ex.: algo.mailinator.com)
        for (String bloqueado : DOMINIOS_DESCARTAVEIS) {
            if (ascii.endsWith("." + bloqueado)) {
                return true;
            }
        }
        return false;
    }

    /**
     * E-mail apto a cadastro: formato + não descartável/reservado.
     * DNS (MX/A) é opcional e feito pelo validador com cache.
     */
    public static boolean isValidoParaCadastro(String email) {
        return isFormatoValido(email) && !isDominioReservadoOuDescartavel(email);
    }
}
