package br.univesp.tcc.cdccnpj.bench.harness;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * Amostra heap e memória comprometida do processo durante a execução (spec §9 dependentes).
 */
final class PeakMemorySampler implements AutoCloseable {

    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final com.sun.management.OperatingSystemMXBean osBean =
            (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private final long[] peakHeap = {0};
    private final long[] peakRss = {0};
    private volatile boolean running;
    private Thread samplerThread;

    static PeakMemorySampler start() {
        PeakMemorySampler sampler = new PeakMemorySampler();
        sampler.running = true;
        sampler.samplerThread = Thread.ofVirtual().name("peak-memory-sampler").start(sampler::sampleLoop);
        return sampler;
    }

    private void sampleLoop() {
        while (running) {
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            if (heapUsed > peakHeap[0]) {
                peakHeap[0] = heapUsed;
            }
            long committed = osBean.getCommittedVirtualMemorySize();
            if (committed > 0 && committed > peakRss[0]) {
                peakRss[0] = committed;
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    long peakHeapBytes() {
        return peakHeap[0];
    }

    long peakRssBytes() {
        return peakRss[0];
    }

    @Override
    public void close() {
        running = false;
        if (samplerThread != null) {
            try {
                samplerThread.join(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
