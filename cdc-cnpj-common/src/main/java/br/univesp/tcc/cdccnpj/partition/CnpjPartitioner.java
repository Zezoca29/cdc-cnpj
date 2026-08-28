package br.univesp.tcc.cdccnpj.partition;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;

/**
 * Partição estável por CNPJ básico (CONTRATOS.md §1, R2).
 */
public final class CnpjPartitioner {

    public static final int BUCKET_COUNT = 256;

    private CnpjPartitioner() {
    }

    public static int bucket(String cnpjBasico) {
        if (cnpjBasico == null || cnpjBasico.length() != 8) {
            throw new IllegalArgumentException("cnpj_basico must be exactly 8 characters");
        }
        int hash = Hashing.murmur3_32().hashString(cnpjBasico, StandardCharsets.UTF_8).asInt();
        return Integer.remainderUnsigned(hash, BUCKET_COUNT);
    }

    public static String bucketPadded(String cnpjBasico) {
        return String.format("%03d", bucket(cnpjBasico));
    }
}
