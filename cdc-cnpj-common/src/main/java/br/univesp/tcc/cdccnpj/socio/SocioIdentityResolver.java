package br.univesp.tcc.cdccnpj.socio;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Resolve identidade estável de sócios (ADR-004, problema P2).
 */
public final class SocioIdentityResolver {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]+");

    private SocioIdentityResolver() {
    }

    public static String identityKey(
            String cnpjBasico, String identificadorSocio, String nomeSocio, String cpfCnpjSocio) {
        String nome = normalizeName(nomeSocio);
        String raw = nullSafe(cnpjBasico) + "|" + nullSafe(identificadorSocio) + "|" + nome;
        return Hashing.murmur3_128().hashString(raw, StandardCharsets.UTF_8).toString();
    }

    public static String identityKeyFromRow(Map<String, String> row) {
        return identityKey(
                row.get("cnpj_basico"),
                row.get("identificador_socio"),
                row.get("nome_socio"),
                row.get("cpf_cnpj_socio"));
    }

    public static boolean sameIdentity(Map<String, String> left, Map<String, String> right) {
        return identityKeyFromRow(left).equals(identityKeyFromRow(right));
    }

    static String normalizeName(String nomeSocio) {
        if (nomeSocio == null || nomeSocio.isBlank()) {
            return "";
        }
        String collapsed = nomeSocio.trim().replaceAll("\\s+", " ");
        String withoutAccents = Normalizer.normalize(collapsed, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String upper = withoutAccents.toUpperCase(Locale.ROOT);
        return NON_ALPHANUMERIC.matcher(upper).replaceAll("");
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
