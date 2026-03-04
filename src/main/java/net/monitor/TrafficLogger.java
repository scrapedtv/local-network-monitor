package net.monitor;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class TrafficLogger {

    private static final int RING_CAP = 2000;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final ArrayDeque<String> ring = new ArrayDeque<>(RING_CAP);
    private final List<Consumer<String>> hooks = new CopyOnWriteArrayList<>();
    private BufferedWriter fw;
    private final Path logFile;

    public TrafficLogger() {
        Path dir = Paths.get(System.getProperty("user.home"), ".netmonitor");
        logFile = dir.resolve("netmonitor.log");
        try {
            Files.createDirectories(dir);
            fw = Files.newBufferedWriter(logFile,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }

    public synchronized void log(String cat, String msg) {
        String ts    = LocalTime.now().format(TS);
        String entry = String.format("%s  %-14s  %s", ts, "[" + cat + "]", msg);

        if (ring.size() >= RING_CAP) ring.pollFirst();
        ring.addLast(entry);

        if (fw != null) {
            try { fw.write(entry); fw.newLine(); fw.flush(); }
            catch (IOException ignored) {}
        }

        for (Consumer<String> h : hooks) {
            try { h.accept(entry); } catch (Exception ignored) {}
        }
    }

    public void onEntry(Consumer<String> hook) {
        hooks.add(hook);
    }

    public synchronized List<String> tail(int n) {
        List<String> all = new ArrayList<>(ring);
        int from = Math.max(0, all.size() - n);
        return all.subList(from, all.size());
    }

    public synchronized void clear() {
        ring.clear();
    }

    public Path logFile() { return logFile; }

    public void close() {
        try { if (fw != null) fw.close(); } catch (IOException ignored) {}
    }
}
