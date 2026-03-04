package net.monitor;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class DeviceScanner {

    private volatile boolean active = false;

    public record DeviceInfo(
        String ip, String hostname, long rttMs,
        boolean up, String method
    ) {}

    public void scan(String subnet, Consumer<DeviceInfo> onResult, Runnable onDone) {
        active = true;
        List<Future<?>> work = new ArrayList<>(254);

        for (int i = 1; i <= 254; i++) {
            if (!active) break;
            final String target = subnet + "." + i;
            work.add(NetworkCore.pool().submit(() -> probe(target, onResult)));
        }

        NetworkCore.pool().submit(() -> {
            for (Future<?> f : work) {
                try { f.get(6, TimeUnit.SECONDS); }
                catch (Exception ignored) {}
            }
            onDone.run();
        });
    }

    private void probe(String target, Consumer<DeviceInfo> out) {
        if (!active) return;

        long start = System.currentTimeMillis();

        try {
            InetAddress addr = InetAddress.getByName(target);
            if (addr.isReachable(1200)) {
                long rtt = System.currentTimeMillis() - start;
                out.accept(new DeviceInfo(target, resolveHost(addr), rtt, true, "ICMP"));
                return;
            }
        } catch (Exception ignored) {}

        int[] tcpPorts = {80, 443, 22, 445, 8080, 21, 23, 3389};
        for (int port : tcpPorts) {
            if (!active) return;
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(target, port), 600);
                long rtt = System.currentTimeMillis() - start;
                try {
                    String host = InetAddress.getByName(target).getCanonicalHostName();
                    out.accept(new DeviceInfo(target, host, rtt, true, "TCP/" + port));
                } catch (Exception e) {
                    out.accept(new DeviceInfo(target, target, rtt, true, "TCP/" + port));
                }
                return;
            } catch (Exception ignored) {}
        }
    }

    private String resolveHost(InetAddress addr) {
        try {
            String h = addr.getCanonicalHostName();
            return h.equals(addr.getHostAddress()) ? addr.getHostAddress() : h;
        } catch (Exception e) {
            return addr.getHostAddress();
        }
    }

    public void stop() {
        active = false;
    }
}
