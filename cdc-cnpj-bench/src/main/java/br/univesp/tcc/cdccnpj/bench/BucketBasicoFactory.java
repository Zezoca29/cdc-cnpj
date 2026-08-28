package br.univesp.tcc.cdccnpj.bench;

import br.univesp.tcc.cdccnpj.partition.CnpjPartitioner;

/**
 * Encontra valores de {@code cnpj_basico} que caem em um bucket específico.
 */
public final class BucketBasicoFactory {

    private BucketBasicoFactory() {
    }

    public static String nthBasicoForBucket(int bucketId, int index) {
        if (bucketId < 0 || bucketId >= CnpjPartitioner.BUCKET_COUNT) {
            throw new IllegalArgumentException("bucketId out of range: " + bucketId);
        }
        int found = 0;
        for (int i = 0; i < 10_000_000; i++) {
            String basico = String.format("%08d", i);
            if (CnpjPartitioner.bucket(basico) == bucketId) {
                if (found == index) {
                    return basico;
                }
                found++;
            }
        }
        throw new IllegalStateException("Could not find basico #" + index + " for bucket " + bucketId);
    }
}
