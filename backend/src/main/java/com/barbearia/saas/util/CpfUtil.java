package com.barbearia.saas.util;

import java.util.Set;

/**
 * Normalização e validação de CPF conforme algoritmo dos dígitos verificadores
 * da Receita Federal do Brasil (módulo 11).
 * <p>
 * Para emissão fiscal, rejeita também CPFs de demonstração conhecidos —
 * o tomador deve informar CPF real cadastrado na Receita Federal.
 */
public final class CpfUtil {

    /**
     * CPFs frequentemente usados em documentação/testes (matematicamente válidos,
     * porém inadequados como tomador real em NFS-e de produção).
     */
    private static final Set<String> CPF_DEMONSTRACAO = Set.of(
            "11144477735",
            "00000000191",
            "12345678909"
    );

    private CpfUtil() {
    }

    /** === Normalização === */

    public static String somenteDigitos(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replaceAll("\\D", "");
    }

    public static boolean pareceCpf(String valor) {
        return somenteDigitos(valor).length() == 11;
    }

    /** === Validação === */

    /** Valida dígitos verificadores (Receita Federal). */
    public static boolean isValido(String valor) {
        String cpf = somenteDigitos(valor);
        if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
            return false;
        }
        try {
            int d1 = digito(cpf, 9, 10);
            int d2 = digito(cpf, 10, 11);
            return cpf.charAt(9) - '0' == d1 && cpf.charAt(10) - '0' == d2;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * CPF apto a figurar como tomador em NFS-e: válido pela Receita e
     * não pertencente à lista de demonstração (quando rejeitarExemplos=true).
     */
    public static boolean isValidoParaNotaFiscal(String valor, boolean rejeitarExemplos) {
        if (!isValido(valor)) {
            return false;
        }
        if (rejeitarExemplos && CPF_DEMONSTRACAO.contains(somenteDigitos(valor))) {
            return false;
        }
        return true;
    }

    public static String formatar(String valor) {
        String c = somenteDigitos(valor);
        if (c.length() != 11) {
            return c;
        }
        return c.substring(0, 3) + "." + c.substring(3, 6) + "." + c.substring(6, 9) + "-" + c.substring(9);
    }

    private static int digito(String cpf, int length, int pesoInicial) {
        int soma = 0;
        int peso = pesoInicial;
        for (int i = 0; i < length; i++) {
            soma += (cpf.charAt(i) - '0') * peso--;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
