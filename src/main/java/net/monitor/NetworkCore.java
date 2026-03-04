package net.monitor;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class NetworkCore {

    private static final ExecutorService POOL = new ThreadPoolExecutor(
        24, 128, 30L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(4096),
        r -> {
            Thread t = new Thread(r, "nm-worker-" + System.nanoTime() % 10000);
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        },
        new ThreadPoolExecutor.CallerRunsPolicy()
    );

    public static ExecutorService pool() {
        return POOL;
    }

    public static List<InterfaceSnapshot> snapInterfaces() {
        List<InterfaceSnapshot> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                if (!ni.isUp()) continue;
                String ip4 = "";
                String mask = "";
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    if (ia.getAddress() instanceof Inet4Address) {
                        ip4 = ia.getAddress().getHostAddress();
                        mask = "/" + ia.getNetworkPrefixLength();
                        break;
                    }
                }
                byte[] hw = ni.getHardwareAddress();
                String mac = hw == null ? "—" : formatMac(hw);
                result.add(new InterfaceSnapshot(
                    ni.getName(), ni.getDisplayName(), ip4, mask, mac,
                    ni.isLoopback(), ni.isVirtual(), ni.getMTU()
                ));
            }
        } catch (SocketException ignored) {}
        return result;
    }

    public static String localIP() {
        try (DatagramSocket sock = new DatagramSocket()) {
            sock.connect(InetAddress.getByName("8.8.8.8"), 80);
            return sock.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    public static String primarySubnet() {
        for (InterfaceSnapshot s : snapInterfaces()) {
            if (!s.loopback() && !s.virtual() && !s.ip4().isEmpty()) {
                String ip = s.ip4();
                return ip.substring(0, ip.lastIndexOf('.'));
            }
        }
        return "192.168.1";
    }

    private static String formatMac(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) sb.append(':');
            sb.append(String.format("%02X", bytes[i]));
        }
        return sb.toString();
    }

    public static void shutdown() {
        POOL.shutdownNow();
    }

    public record InterfaceSnapshot(
        String name, String display, String ip4, String mask,
        String mac, boolean loopback, boolean virtual, int mtu
    ) {}
}
