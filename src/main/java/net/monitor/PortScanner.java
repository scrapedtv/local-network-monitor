package net.monitor;

import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class PortScanner {

    public record PortInfo(int port, String service, boolean open, long rttMs) {}

    private volatile boolean active = false;

    private static final Map<Integer, String> REGISTRY = new LinkedHashMap<>() {{
        put(  21, "FTP");         put(  22, "SSH");        put(  23, "Telnet");
        put(  25, "SMTP");        put(  53, "DNS");         put(  67, "DHCP");
        put(  80, "HTTP");        put( 110, "POP3");        put( 123, "NTP");
        put( 143, "IMAP");        put( 161, "SNMP");        put( 389, "LDAP");
        put( 443, "HTTPS");       put( 445, "SMB");         put( 465, "SMTPS");
        put( 514, "Syslog");      put( 587, "SMTP-Sub");    put( 636, "LDAPS");
        put( 993, "IMAPS");       put( 995, "POP3S");       put(1194, "OpenVPN");
        put(1433, "MSSQL");       put(1521, "Oracle");      put(3306, "MySQL");
        put(3389, "RDP");         put(5432, "PostgreSQL");  put(5900, "VNC");
        put(6379, "Redis");       put(8080, "HTTP-Alt");    put(8443, "HTTPS-Alt");
        put(8888, "HTTP-Dev");    put(9200, "Elastic");     put(9300, "Elastic-T");
        put(27017,"MongoDB");     put(5353, "mDNS");        put(2375, "Docker");
        put(2376, "Docker-TLS"); put(4443, "Alt-HTTPS");   put(6443, "K8s-API");
    }};

    public static List<Integer> commonPorts() {
        return new ArrayList<>(REGISTRY.keySet());
    }

    public static List<Integer> rangePorts(int from, int to) {
        List<Integer> ports = new ArrayList<>(to - from + 1);
        for (int p = from; p <= to; p++) ports.add(p);
        return ports;
    }

    public static Map<Integer, String> registry() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    public void scan(String host, List<Integer> ports, int timeoutMs,
                     Consumer<PortInfo> onResult, Runnable onDone) {
        active = true;
        List<Future<?>> work = new ArrayList<>(ports.size());

        for (int port : ports) {
            if (!active) break;
            final int p = port;
            work.add(NetworkCore.pool().submit(() -> {
                if (!active) return;
                long start = System.currentTimeMillis();
                boolean open = nioProbe(host, p, timeoutMs);
                long rtt = System.currentTimeMillis() - start;
                onResult.accept(new PortInfo(p, REGISTRY.getOrDefault(p, "—"), open, rtt));
            }));
        }

        NetworkCore.pool().submit(() -> {
            for (Future<?> f : work) {
                try { f.get(timeoutMs + 2000L, TimeUnit.MILLISECONDS); }
                catch (Exception ignored) {}
            }
            onDone.run();
        });
    }

    private boolean nioProbe(String host, int port, int timeoutMs) {
        try (SocketChannel ch = SocketChannel.open()) {
            ch.configureBlocking(false);
            ch.connect(new InetSocketAddress(host, port));
            try (Selector sel = Selector.open()) {
                ch.register(sel, SelectionKey.OP_CONNECT);
                if (sel.select(timeoutMs) == 0) return false;
                Set<SelectionKey> keys = sel.selectedKeys();
                for (SelectionKey k : keys) {
                    if (k.isConnectable()) {
                        try { ch.finishConnect(); return true; }
                        catch (Exception e) { return false; }
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public void stop() {
        active = false;
    }
}
