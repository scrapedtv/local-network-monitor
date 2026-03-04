package net.monitor;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class PingService {

    public record PingResult(String host, long rttMs, boolean success, long ts) {}

    public static class Stats {
        private final List<Long> rtts = Collections.synchronizedList(new ArrayList<>());
        private int sent = 0, recv = 0;

        synchronized void record(PingResult r) {
            sent++;
            if (r.success()) { recv++; rtts.add(r.rttMs()); }
        }

        public long min()     { return rtts.isEmpty() ? 0 : rtts.stream().mapToLong(Long::longValue).min().orElse(0); }
        public long max()     { return rtts.isEmpty() ? 0 : rtts.stream().mapToLong(Long::longValue).max().orElse(0); }
        public double avg()   { return rtts.isEmpty() ? 0 : rtts.stream().mapToLong(Long::longValue).average().orElse(0); }
        public double loss()  { return sent == 0 ? 0 : (double)(sent - recv) / sent * 100.0; }
        public int sent()     { return sent; }
        public int recv()     { return recv; }
        public List<Long> rtts() { return Collections.unmodifiableList(new ArrayList<>(rtts)); }

        synchronized void reset() { rtts.clear(); sent = 0; recv = 0; }
    }

    private volatile boolean running = false;
    private ScheduledExecutorService sched;
    private final Stats stats = new Stats();

    public void start(String host, int intervalMs, Consumer<PingResult> callback) {
        stop();
        stats.reset();
        running = true;

        sched = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ping-loop");
            t.setDaemon(true);
            return t;
        });

        sched.scheduleAtFixedRate(() -> {
            if (!running) return;
            long start = System.currentTimeMillis();
            boolean ok;
            try {
                ok = InetAddress.getByName(host).isReachable(2500);
            } catch (Exception e) {
                ok = false;
            }
            long rtt = System.currentTimeMillis() - start;
            PingResult r = new PingResult(host, ok ? rtt : -1, ok, System.currentTimeMillis());
            stats.record(r);
            callback.accept(r);
        }, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        running = false;
        if (sched != null && !sched.isShutdown()) sched.shutdownNow();
    }

    public Stats stats() { return stats; }
}
