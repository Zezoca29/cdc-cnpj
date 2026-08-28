package br.univesp.tcc.cdccnpj.event;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Geração determinística de event_id (CONTRATOS.md §4.2, R6).
 */
public final class EventIdGenerator {

    public static final UUID NAMESPACE_CDC_CNPJ =
            UUID.fromString("1f7a95b3-e59c-5ed5-af9e-7e2d84a7a8cd");

    private EventIdGenerator() {
    }

    public static UUID generate(
            String competenciaDe,
            String competenciaPara,
            Map<String, String> entityKey,
            String eventType,
            List<String> camposAlterados) {
        String canonical = buildCanonicalString(
                competenciaDe, competenciaPara, entityKey, eventType, camposAlterados);
        return uuidV5(NAMESPACE_CDC_CNPJ, canonical);
    }

    public static String buildCanonicalString(
            String competenciaDe,
            String competenciaPara,
            Map<String, String> entityKey,
            String eventType,
            List<String> camposAlterados) {
        String entityCanonical = entityKey.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        String fields = camposAlterados == null || camposAlterados.isEmpty()
                ? ""
                : camposAlterados.stream().sorted().collect(Collectors.joining(","));
        return String.join("|", competenciaDe, competenciaPara, entityCanonical, eventType, fields);
    }

    static UUID uuidV5(UUID namespace, String name) {
        byte[] namespaceBytes = uuidToBytes(namespace);
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        byte[] input = new byte[namespaceBytes.length + nameBytes.length];
        System.arraycopy(namespaceBytes, 0, input, 0, namespaceBytes.length);
        System.arraycopy(nameBytes, 0, input, namespaceBytes.length, nameBytes.length);

        byte[] hash;
        try {
            hash = java.security.MessageDigest.getInstance("SHA-1").digest(input);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 not available", e);
        }
        hash[6] &= 0x0f;
        hash[6] |= 0x50;
        hash[8] &= 0x3f;
        hash[8] |= 0x80;
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (hash[i] & 0xff);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (hash[i] & 0xff);
        }
        return new UUID(msb, lsb);
    }

    private static byte[] uuidToBytes(UUID uuid) {
        byte[] bytes = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (msb >>> (8 * (7 - i)));
        }
        for (int i = 8; i < 16; i++) {
            bytes[i] = (byte) (lsb >>> (8 * (7 - (i - 8))));
        }
        return bytes;
    }
}
