package com.barbearia.saas.util;

/**
 * Normalização e validação de CNPJ conforme algoritmo dos dígitos verificadores
 * da Receita Federal do Brasil (módulo 11).
 */
public final class CnpjUtil {

    private static final int[] PESO1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESO2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CnpjUtil() {
    }

    /** === Normalização === */

    public static String somenteDigitos(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replaceAll("\\D", "");
    }

    /** === Validação === */

    /** Valida dígitos verificadores (Receita Federal). */
    public static boolean isValido(String valor) {
        String cnpj = somenteDigitos(valor);
        if (cnpj.length() != 14 || cnpj.chars().distinct().count() == 1) {
            return false;
        }
        try {
            int d1 = digito(cnpj, PESO1);
            int d2 = digito(cnpj.substring(0, 12) + d1, PESO2);
            return cnpj.charAt(12) - '0' == d1 && cnpj.charAt(13) - '0' == d2;
        } catch (Exception e) {
            return false;
        }
    }

    public static String formatar(String valor) {
        String c = somenteDigitos(valor);
        if (c.length() != 14) {
            return c;
        }
        return c.substring(0, 2) + "." + c.substring(2, 5) + "." + c.substring(5, 8)
                + "/" + c.substring(8, 12) + "-" + c.substring(12);
    }

    private static int digito(String base, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (base.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
