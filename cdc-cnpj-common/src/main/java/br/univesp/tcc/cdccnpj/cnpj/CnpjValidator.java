package br.univesp.tcc.cdccnpj.cnpj;

/**
 * Validação de CNPJ alfanumérico — formato misto (CONTRATOS.md §2).
 * Valor do caractere para DV = código ASCII − 48.
 */
public final class CnpjValidator {

    private static final int[] WEIGHTS_DV1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_DV2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CnpjValidator() {
    }

    public static boolean isValid(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) {
            return false;
        }
        for (int i = 0; i < 14; i++) {
            if (!isAllowedChar(cnpj.charAt(i))) {
                return false;
            }
        }
        String base12 = cnpj.substring(0, 12);
        String expectedDv = computeCheckDigits(base12);
        return cnpj.substring(12).equals(expectedDv);
    }

    public static String compose(String cnpjBasico, String cnpjOrdem, String cnpjDv) {
        if (cnpjBasico == null || cnpjBasico.length() != 8) {
            throw new IllegalArgumentException("cnpj_basico must be 8 characters");
        }
        if (cnpjOrdem == null || cnpjOrdem.length() != 4) {
            throw new IllegalArgumentException("cnpj_ordem must be 4 characters");
        }
        if (cnpjDv == null || cnpjDv.length() != 2) {
            throw new IllegalArgumentException("cnpj_dv must be 2 characters");
        }
        return cnpjBasico + cnpjOrdem + cnpjDv;
    }

    public static String computeCheckDigits(String base12) {
        if (base12 == null || base12.length() != 12) {
            throw new IllegalArgumentException("base must be 12 characters");
        }
        int dv1 = computeDigit(base12, WEIGHTS_DV1);
        int dv2 = computeDigit(base12 + dv1, WEIGHTS_DV2);
        return String.valueOf(dv1) + dv2;
    }

    private static int computeDigit(String value, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += charValue(value.charAt(i)) * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    static int charValue(char c) {
        return c - 48;
    }

    private static boolean isAllowedChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }
}
