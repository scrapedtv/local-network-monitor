package net.monitor;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;

public class MonitorUI extends JFrame {

    static final Color BG     = new Color( 10,  14,  26);
    static final Color PANEL  = new Color( 16,  20,  35);
    static final Color CARD   = new Color( 22,  27,  45);
    static final Color BORDER = new Color( 48,  54,  61);
    static final Color ACCENT = new Color(  0, 212, 170);
    static final Color TEXT   = new Color(230, 237, 243);
    static final Color MUTED  = new Color(139, 148, 158);
    static final Color OK     = new Color( 63, 185,  80);
    static final Color WARN   = new Color(240, 165,   0);
    static final Color ERR    = new Color(248,  81,  73);

    static final Font MONO   = resolveFont(12, Font.MONOSPACED, "JetBrains Mono", "Fira Code", "Consolas");
    static final Font MONO_B = MONO.deriveFont(Font.BOLD);
    static final Font UI     = resolveFont(13, Font.SANS_SERIF, "Segoe UI", "Inter", "Helvetica Neue");
    static final Font UI_B   = UI.deriveFont(Font.BOLD);
    static final Font UI_S   = UI.deriveFont(11f);
    static final Font LOGO   = resolveFont(15, Font.SANS_SERIF, "Segoe UI", "Inter").deriveFont(Font.BOLD);

    private static Font resolveFont(int size, String fallback, String... prefs) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Set<String> av = new HashSet<>(Arrays.asList(ge.getAvailableFontFamilyNames()));
        for (String n : prefs) if (av.contains(n)) return new Font(n, Font.PLAIN, size);
        return new Font(fallback, Font.PLAIN, size);
    }

    private final TrafficLogger logger = new TrafficLogger();
    private DeviceScanner devScanner;
    private PortScanner   portScanner;
    private PingService   pingSvc;

    private DefaultTableModel devModel, portModel, ifModel;
    private JTextArea  logArea;
    private JLabel     statusL, clockL, pingStatsL;
    private JProgressBar scanProgress;
    private PingGraphPanel pingGraph;
    private JButton    devScanBtn, portScanBtn, pingStartBtn;
    private JTextField devSubnetF, portHostF, portFromF, portToF, pingHostF;

    private final CardLayout cards  = new CardLayout();
    private final JPanel     center = new JPanel(cards);
    private final List<NavBtn> navBtns = new ArrayList<>();

    record NavBtn(JButton btn, String card) {}

    public MonitorUI() {
        super("NetMonitor");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1320, 840));
        setMinimumSize(new Dimension(1020, 660));

        buildUI();
        pack();
        setLocationRelativeTo(null);

        logger.log("SYSTEM", "NetMonitor online — local " + NetworkCore.localIP());
        logger.onEntry(e -> SwingUtilities.invokeLater(() -> appendLog(e)));
        startClock();

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (devScanner  != null) devScanner.stop();
                if (portScanner != null) portScanner.stop();
                if (pingSvc     != null) pingSvc.stop();
                logger.close();
                NetworkCore.shutdown();
            }
        });
    }

    private void buildUI() {
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
        add(header(),    BorderLayout.NORTH);
        add(sidebar(),   BorderLayout.WEST);
        add(mainArea(),  BorderLayout.CENTER);
        add(statusBar(), BorderLayout.SOUTH);
    }

    // ── HEADER ────────────────────────────────────────────────────────────────
    private JPanel header() {
        JPanel p = darkPanel(new BorderLayout());
        p.setPreferredSize(new Dimension(0, 52));
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));

        JPanel left = darkPanel(new FlowLayout(FlowLayout.LEFT, 20, 14));
        JLabel logo = label("◈  NETMONITOR", LOGO, ACCENT);
        JLabel ver  = label("v1.0", UI_S, MUTED);
        left.add(logo); left.add(ver);

        JPanel right = darkPanel(new FlowLayout(FlowLayout.RIGHT, 20, 14));
        JLabel ipL  = label("  " + NetworkCore.localIP() + "  ", MONO, ACCENT);
        ipL.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 212, 170, 60)),
            new EmptyBorder(1, 6, 1, 6)));
        clockL = label("", MONO, MUTED);
        right.add(ipL); right.add(clockL);

        p.add(left, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    // ── SIDEBAR ───────────────────────────────────────────────────────────────
    private JPanel sidebar() {
        JPanel p = darkPanel(null);
        p.setBackground(PANEL);
        p.setPreferredSize(new Dimension(192, 0));
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        p.add(Box.createVerticalStrut(20));
        addNav(p, "  Dashboard",  "DASH");
        addNav(p, "  Interfaces", "IFACE");
        addNav(p, "  Devices",    "DEV");
        addNav(p, "  Port Scan",  "PORT");
        addNav(p, "  Ping",       "PING");
        addNav(p, "  Logs",       "LOG");
        p.add(Box.createVerticalGlue());

        JLabel sub = label("  " + NetworkCore.primarySubnet() + ".0/24", UI_S, new Color(80, 90, 110));
        sub.setBorder(new EmptyBorder(0, 0, 10, 0));
        p.add(sub);

        return p;
    }

    private void addNav(JPanel sidebar, String text, String card) {
        JButton btn = new JButton(text) {
            { setOpaque(true); setFocusPainted(false); setBorderPainted(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                boolean active = navBtns.stream().anyMatch(n -> n.card().equals(card) && n.btn() == this && isSelected());
                boolean hover  = getModel().isRollover();
                g2.setColor(active ? CARD : hover ? new Color(28, 34, 52) : PANEL);
                g2.fillRect(0, 0, getWidth(), getHeight());
                if (active) { g2.setColor(ACCENT); g2.fillRect(0, 0, 3, getHeight()); }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(UI);
        btn.setForeground(MUTED);
        btn.setBackground(PANEL);
        btn.setMaximumSize(new Dimension(192, 44));
        btn.setPreferredSize(new Dimension(192, 44));
        btn.setHorizontalAlignment(SwingConstants.LEFT);

        btn.addActionListener(e -> {
            navBtns.forEach(n -> { n.btn().setSelected(false); n.btn().setForeground(MUTED); });
            btn.setSelected(true);
            btn.setForeground(TEXT);
            cards.show(center, card);
            btn.repaint();
        });

        NavBtn nb = new NavBtn(btn, card);
        navBtns.add(nb);
        if (navBtns.size() == 1) { btn.setSelected(true); btn.setForeground(TEXT); }
        sidebar.add(btn);
    }

    // ── MAIN AREA ─────────────────────────────────────────────────────────────
    private JPanel mainArea() {
        center.setBackground(BG);
        center.add(dashPanel(),  "DASH");
        center.add(ifacePanel(), "IFACE");
        center.add(devPanel(),   "DEV");
        center.add(portPanel(),  "PORT");
        center.add(pingPanel(),  "PING");
        center.add(logPanel(),   "LOG");
        return center;
    }

    // ── DASHBOARD ─────────────────────────────────────────────────────────────
    private JPanel dashPanel() {
        JPanel root = scrollable(new BorderLayout(0, 16));
        root.setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel metrics = darkPanel(new GridLayout(1, 4, 12, 0));
        long ifaceCount = NetworkCore.snapInterfaces().stream().filter(i -> !i.loopback()).count();
        metrics.add(metricCard("Local IP",    NetworkCore.localIP(),           ACCENT));
        metrics.add(metricCard("Subnet",      NetworkCore.primarySubnet() + ".x", new Color(180, 130, 255)));
        metrics.add(metricCard("Interfaces",  String.valueOf(ifaceCount),      OK));
        metrics.add(metricCard("Log File",    "~/.netmonitor/",               WARN));

        JPanel top = darkPanel(new BorderLayout(0, 8));
        top.add(sectionLabel("Overview"), BorderLayout.NORTH);
        top.add(metrics, BorderLayout.CENTER);

        String[] cols = {"Interface", "Display Name", "IPv4", "Mask", "MAC", "MTU", "Type"};
        ifModel = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        NetworkCore.snapInterfaces().forEach(i -> ifModel.addRow(new Object[]{
            i.name(), i.display(), i.ip4().isEmpty() ? "—" : i.ip4(), i.mask(),
            i.mac(), i.mtu() > 0 ? i.mtu() : "—",
            i.loopback() ? "loopback" : i.virtual() ? "virtual" : "physical"
        }));

        JPanel tableWrap = darkPanel(new BorderLayout(0, 8));
        tableWrap.add(sectionLabel("Network Interfaces"), BorderLayout.NORTH);
        tableWrap.add(styledScroll(styledTable(ifModel)), BorderLayout.CENTER);

        root.add(top,       BorderLayout.NORTH);
        root.add(tableWrap, BorderLayout.CENTER);
        return root;
    }

    // ── INTERFACES ────────────────────────────────────────────────────────────
    private JPanel ifacePanel() {
        JPanel root = scrollable(new BorderLayout(0, 16));
        root.setBorder(new EmptyBorder(24, 24, 24, 24));
        root.add(sectionLabel("Live Interfaces"), BorderLayout.NORTH);

        String[] cols = {"Interface", "IP / Mask", "MAC Address", "MTU", "Up?", "Loopback", "Virtual"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        NetworkCore.snapInterfaces().forEach(i -> model.addRow(new Object[]{
            i.name(), i.ip4() + i.mask(), i.mac(), i.mtu(),
            "●", i.loopback() ? "yes" : "no", i.virtual() ? "yes" : "no"
        }));

        JTable tbl = styledTable(model);
        tbl.getColumnModel().getColumn(4).setCellRenderer(dotRenderer());
        root.add(styledScroll(tbl), BorderLayout.CENTER);
        return root;
    }

    // ── DEVICES ───────────────────────────────────────────────────────────────
    private JPanel devPanel() {
        JPanel root = darkPanel(new BorderLayout(0, 0));
        root.setBorder(new EmptyBorder(24, 24, 24, 24));

        devSubnetF = darkField(NetworkCore.primarySubnet());
        devScanBtn = accentBtn("▶  Scan Subnet");
        JButton stopBtn = ghostBtn("■  Stop");
        scanProgress = new JProgressBar();
        scanProgress.setIndeterminate(false);
        scanProgress.setBackground(CARD);
        scanProgress.setForeground(ACCENT);
        scanProgress.setBorder(BorderFactory.createLineBorder(BORDER));
        scanProgress.setPreferredSize(new Dimension(160, 20));

        JPanel toolbar = darkPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.add(label("Subnet:", UI, MUTED));
        toolbar.add(devSubnetF);
        toolbar.add(darkLabel(".1 – .254"));
        toolbar.add(devScanBtn);
        toolbar.add(stopBtn);
        toolbar.add(scanProgress);

        String[] cols = {"IP Address", "Hostname", "RTT (ms)", "Status", "Method"};
        devModel = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };

        JTable tbl = styledTable(devModel);
        tbl.getColumnModel().getColumn(3).setCellRenderer(statusRenderer());

        devScanBtn.addActionListener(e -> {
            devModel.setRowCount(0);
            scanProgress.setIndeterminate(true);
            devScanBtn.setEnabled(false);
            devScanner = new DeviceScanner();
            String subnet = devSubnetF.getText().trim();
            logger.log("DEV-SCAN", "Scanning " + subnet + ".1-254");
            devScanner.scan(subnet,
                r -> SwingUtilities.invokeLater(() -> {
                    if (r.up()) devModel.addRow(new Object[]{
                        r.ip(), r.hostname(), r.rttMs(), "UP", r.method()
                    });
                }),
                () -> SwingUtilities.invokeLater(() -> {
                    scanProgress.setIndeterminate(false);
                    devScanBtn.setEnabled(true);
                    logger.log("DEV-SCAN", "Done — " + devModel.getRowCount() + " devices found");
                    setStatus("Scan complete: " + devModel.getRowCount() + " devices");
                })
            );
        });

        stopBtn.addActionListener(e -> {
            if (devScanner != null) devScanner.stop();
            scanProgress.setIndeterminate(false);
            devScanBtn.setEnabled(true);
        });

        JPanel wrap = darkPanel(new BorderLayout(0, 10));
        wrap.add(sectionLabel("Device Discovery"), BorderLayout.NORTH);
        wrap.add(toolbar, BorderLayout.CENTER);

        root.add(wrap,             BorderLayout.NORTH);
        root.add(styledScroll(tbl), BorderLayout.CENTER);
        return root;
    }

    // ── PORT SCAN ─────────────────────────────────────────────────────────────
    private JPanel portPanel() {
        JPanel root = darkPanel(new BorderLayout(0, 0));
        root.setBorder(new EmptyBorder(24, 24, 24, 24));

        portHostF = darkField("192.168.1.1");
        portFromF = darkField("1");
        portToF   = darkField("1024");
        portScanBtn = accentBtn("▶  Scan Ports");
        JButton stopBtn   = ghostBtn("■  Stop");
        JButton commonBtn = ghostBtn("Common Ports");

        JProgressBar pbar = new JProgressBar();
        pbar.setIndeterminate(false);
        pbar.setBackground(CARD);
        pbar.setForeground(ACCENT);
        pbar.setBorder(BorderFactory.createLineBorder(BORDER));
        pbar.setPreferredSize(new Dimension(160, 20));

        JPanel toolbar = darkPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.add(label("Host:", UI, MUTED)); toolbar.add(portHostF);
        toolbar.add(label("Port:", UI, MUTED)); toolbar.add(portFromF);
        toolbar.add(darkLabel("—")); toolbar.add(portToF);
        toolbar.add(portScanBtn); toolbar.add(stopBtn); toolbar.add(commonBtn); toolbar.add(pbar);

        String[] cols = {"Port", "Service", "Status", "RTT (ms)"};
        portModel = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };

        JTable tbl = styledTable(portModel);
        tbl.getColumnModel().getColumn(2).setCellRenderer(statusRenderer());

        portScanBtn.addActionListener(e -> {
            portModel.setRowCount(0);
            pbar.setIndeterminate(true);
            portScanBtn.setEnabled(false);
            portScanner = new PortScanner();
            String host = portHostF.getText().trim();
            int from = intVal(portFromF.getText(), 1);
            int to   = intVal(portToF.getText(), 1024);
            List<Integer> ports = PortScanner.rangePorts(from, to);
            logger.log("PORT-SCAN", "Scanning " + host + " ports " + from + "-" + to);
            portScanner.scan(host, ports, 1500,
                r -> SwingUtilities.invokeLater(() -> {
                    if (r.open()) portModel.addRow(new Object[]{r.port(), r.service(), "OPEN", r.rttMs()});
                }),
                () -> SwingUtilities.invokeLater(() -> {
                    pbar.setIndeterminate(false);
                    portScanBtn.setEnabled(true);
                    logger.log("PORT-SCAN", "Done — " + portModel.getRowCount() + " open ports on " + host);
                    setStatus("Port scan done: " + portModel.getRowCount() + " open ports");
                })
            );
        });

        commonBtn.addActionListener(e -> {
            portModel.setRowCount(0);
            pbar.setIndeterminate(true);
            portScanBtn.setEnabled(false);
            portScanner = new PortScanner();
            String host = portHostF.getText().trim();
            logger.log("PORT-SCAN", "Common port scan on " + host);
            portScanner.scan(host, PortScanner.commonPorts(), 1500,
                r -> SwingUtilities.invokeLater(() ->
                    portModel.addRow(new Object[]{r.port(), r.service(), r.open() ? "OPEN" : "CLOSED", r.rttMs()})),
                () -> SwingUtilities.invokeLater(() -> {
                    pbar.setIndeterminate(false);
                    portScanBtn.setEnabled(true);
                    setStatus("Common scan done");
                })
            );
        });

        stopBtn.addActionListener(e -> {
            if (portScanner != null) portScanner.stop();
            pbar.setIndeterminate(false);
            portScanBtn.setEnabled(true);
        });

        JPanel header = darkPanel(new BorderLayout(0, 10));
        header.add(sectionLabel("Port Scanner"), BorderLayout.NORTH);
        header.add(toolbar, BorderLayout.CENTER);

        root.add(header,            BorderLayout.NORTH);
        root.add(styledScroll(tbl), BorderLayout.CENTER);
        return root;
    }

    // ── PING ──────────────────────────────────────────────────────────────────
    private JPanel pingPanel() {
        JPanel root = darkPanel(new BorderLayout(0, 16));
        root.setBorder(new EmptyBorder(24, 24, 24, 24));

        pingHostF   = darkField("8.8.8.8");
        pingStartBtn = accentBtn("▶  Start");
        JButton stopBtn = ghostBtn("■  Stop");
        JComboBox<String> intervalBox = new JComboBox<>(new String[]{"500ms", "1s", "2s", "5s"});
        intervalBox.setBackground(CARD);
        intervalBox.setForeground(TEXT);
        intervalBox.setFont(MONO);

        JPanel toolbar = darkPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.add(label("Host:", UI, MUTED));
        toolbar.add(pingHostF);
        toolbar.add(label("Interval:", UI, MUTED));
        toolbar.add(intervalBox);
        toolbar.add(pingStartBtn);
        toolbar.add(stopBtn);

        pingGraph  = new PingGraphPanel();
        pingStatsL = label("Waiting…", MONO, MUTED);
        pingStatsL.setBorder(new EmptyBorder(8, 0, 0, 0));

        pingStartBtn.addActionListener(e -> {
            String host = pingHostF.getText().trim();
            String sel  = (String) intervalBox.getSelectedItem();
            int ms = switch (sel) { case "500ms" -> 500; case "2s" -> 2000; case "5s" -> 5000; default -> 1000; };
            pingGraph.reset();
            pingSvc = new PingService();
            logger.log("PING", "Start → " + host + " every " + sel);
            pingStartBtn.setEnabled(false);
            pingSvc.start(host, ms, r -> SwingUtilities.invokeLater(() -> {
                pingGraph.push(r.rttMs());
                PingService.Stats s = pingSvc.stats();
                pingStatsL.setText(String.format(
                    "Sent: %d   Recv: %d   Loss: %.1f%%   Min: %dms   Avg: %.1fms   Max: %dms",
                    s.sent(), s.recv(), s.loss(), s.min(), s.avg(), s.max()
                ));
                pingStatsL.setForeground(r.success() ? TEXT : ERR);
                logger.log("PING", (r.success() ? "rtt=" + r.rttMs() + "ms" : "TIMEOUT") + " host=" + host);
            }));
        });

        stopBtn.addActionListener(e -> {
            if (pingSvc != null) pingSvc.stop();
            pingStartBtn.setEnabled(true);
            logger.log("PING", "Stopped");
        });

        JPanel header = darkPanel(new BorderLayout(0, 10));
        header.add(sectionLabel("Ping Monitor"), BorderLayout.NORTH);
        header.add(toolbar, BorderLayout.CENTER);

        root.add(header,    BorderLayout.NORTH);
        root.add(pingGraph, BorderLayout.CENTER);
        root.add(pingStatsL, BorderLayout.SOUTH);
        return root;
    }

    // ── LOGS ──────────────────────────────────────────────────────────────────
    private JPanel logPanel() {
        JPanel root = darkPanel(new BorderLayout(0, 0));
        root.setBorder(new EmptyBorder(24, 24, 24, 24));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(8, 11, 20));
        logArea.setForeground(new Color(160, 200, 160));
        logArea.setFont(MONO);
        logArea.setLineWrap(false);
        logArea.setCaretColor(ACCENT);
        logArea.setBorder(new EmptyBorder(12, 12, 12, 12));

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBackground(new Color(8, 11, 20));
        scroll.getViewport().setBackground(new Color(8, 11, 20));
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));

        JButton clearBtn = ghostBtn("Clear");
        JButton fileBtn  = ghostBtn("Open Log File");

        clearBtn.addActionListener(e -> { logger.clear(); logArea.setText(""); });
        fileBtn.addActionListener(e -> {
            try { Desktop.getDesktop().open(logger.logFile().toFile()); }
            catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
        });

        JPanel toolbar = darkPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.add(label("Event Log  ·  " + logger.logFile(), UI_S, MUTED));
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(clearBtn);
        toolbar.add(fileBtn);

        JPanel header = darkPanel(new BorderLayout(0, 10));
        header.add(sectionLabel("Logs"), BorderLayout.NORTH);
        header.add(toolbar, BorderLayout.CENTER);

        root.add(header, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);

        logger.tail(200).forEach(this::appendLog);
        return root;
    }

    // ── STATUS BAR ────────────────────────────────────────────────────────────
    private JPanel statusBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(8, 11, 20));
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));
        p.setPreferredSize(new Dimension(0, 26));

        statusL = label("  Ready", UI_S, MUTED);
        JLabel right = label(NetworkCore.primarySubnet() + ".0/24  ", UI_S, new Color(60, 80, 100));

        p.add(statusL, BorderLayout.WEST);
        p.add(right,   BorderLayout.EAST);
        return p;
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────
    private JPanel darkPanel(LayoutManager lm) {
        JPanel p = lm == null ? new JPanel() : new JPanel(lm);
        p.setBackground(BG);
        return p;
    }

    private JPanel scrollable(LayoutManager lm) {
        JPanel p = darkPanel(lm);
        p.setBackground(BG);
        return p;
    }

    private JLabel label(String t, Font f, Color c) {
        JLabel l = new JLabel(t);
        l.setFont(f); l.setForeground(c);
        return l;
    }

    private JLabel darkLabel(String t) { return label(t, UI, MUTED); }

    private JLabel sectionLabel(String t) {
        JLabel l = label(t, UI_B, new Color(180, 190, 210));
        l.setBorder(new EmptyBorder(0, 0, 12, 0));
        return l;
    }

    private JTextField darkField(String text) {
        JTextField f = new JTextField(text, 14);
        f.setBackground(CARD);
        f.setForeground(TEXT);
        f.setFont(MONO);
        f.setCaretColor(ACCENT);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(3, 8, 3, 8)));
        return f;
    }

    private JButton accentBtn(String t) {
        JButton b = new JButton(t) {
            { setOpaque(true); setFocusPainted(false); setBorderPainted(false); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(getModel().isRollover() ? new Color(0, 180, 145) : ACCENT);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(UI_B); b.setForeground(new Color(10, 14, 26));
        b.setBackground(ACCENT);
        b.setBorder(new EmptyBorder(6, 16, 6, 16));
        return b;
    }

    private JButton ghostBtn(String t) {
        JButton b = new JButton(t) {
            { setOpaque(true); setFocusPainted(false); setBorderPainted(true); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
        };
        b.setFont(UI); b.setForeground(MUTED);
        b.setBackground(CARD);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(5, 14, 5, 14)));
        return b;
    }

    private JTable styledTable(DefaultTableModel model) {
        JTable tbl = new JTable(model);
        tbl.setBackground(CARD);
        tbl.setForeground(TEXT);
        tbl.setFont(MONO);
        tbl.setGridColor(new Color(32, 38, 58));
        tbl.setRowHeight(26);
        tbl.setShowHorizontalLines(true);
        tbl.setShowVerticalLines(false);
        tbl.setSelectionBackground(new Color(0, 212, 170, 40));
        tbl.setSelectionForeground(TEXT);
        tbl.setFillsViewportHeight(true);
        tbl.getTableHeader().setBackground(new Color(14, 18, 32));
        tbl.getTableHeader().setForeground(MUTED);
        tbl.getTableHeader().setFont(UI_B);
        tbl.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        tbl.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBackground(sel ? new Color(0, 212, 170, 40) : row % 2 == 0 ? CARD : new Color(18, 23, 40));
                setForeground(TEXT);
                setFont(MONO);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        });
        return tbl;
    }

    private JScrollPane styledScroll(Component c) {
        JScrollPane s = new JScrollPane(c);
        s.setBackground(CARD);
        s.getViewport().setBackground(CARD);
        s.setBorder(BorderFactory.createLineBorder(BORDER));
        return s;
    }

    private TableCellRenderer statusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                String s = v == null ? "" : v.toString();
                setForeground("UP".equals(s) || "OPEN".equals(s) ? OK : "CLOSED".equals(s) ? MUTED : ERR);
                setBackground(sel ? new Color(0, 212, 170, 40) : row % 2 == 0 ? CARD : new Color(18, 23, 40));
                setFont(MONO_B);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                setText("● " + s);
                return this;
            }
        };
    }

    private TableCellRenderer dotRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setForeground(OK);
                setBackground(sel ? new Color(0, 212, 170, 40) : row % 2 == 0 ? CARD : new Color(18, 23, 40));
                setFont(MONO_B);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        };
    }

    private JPanel metricCard(String title, String value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(16, 20, 16, 20)));
        JLabel t = label(title.toUpperCase(), UI_S, MUTED);
        JLabel v = label(value, LOGO, accent);
        card.add(t, BorderLayout.NORTH);
        card.add(v, BorderLayout.CENTER);
        return card;
    }

    private void appendLog(String entry) {
        if (logArea == null) return;
        logArea.append(entry + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void setStatus(String msg) {
        SwingUtilities.invokeLater(() -> statusL.setText("  " + msg));
    }

    private void startClock() {
        Timer t = new Timer(1000, e -> {
            String ts = DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalTime.now());
            clockL.setText(ts);
        });
        t.start();
    }

    private int intVal(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    // ── PING GRAPH ────────────────────────────────────────────────────────────
    static final class PingGraphPanel extends JPanel {

        private static final int CAP = 60;
        private final ArrayDeque<Long> pts = new ArrayDeque<>(CAP);
        private long ceiling = 200;

        PingGraphPanel() {
            setBackground(CARD);
            setBorder(BorderFactory.createLineBorder(BORDER));
        }

        void push(long rtt) {
            if (pts.size() >= CAP) pts.pollFirst();
            pts.addLast(rtt);
            if (rtt > 0 && rtt > ceiling * 0.8) ceiling = (long)(rtt * 1.5);
            repaint();
        }

        void reset() {
            pts.clear();
            ceiling = 200;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int W = getWidth(), H = getHeight();
            int pad = 46, gW = W - pad - 16, gH = H - pad - 20;
            if (gW < 10 || gH < 10) { g2.dispose(); return; }

            g2.setColor(CARD);
            g2.fillRect(0, 0, W, H);

            Stroke dash = new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{4f, 4f}, 0f);
            g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));

            for (int i = 0; i <= 4; i++) {
                int y = pad + gH * i / 4;
                g2.setColor(new Color(40, 46, 66));
                g2.setStroke(dash);
                g2.drawLine(pad, y, pad + gW, y);
                long lbl = ceiling * (4 - i) / 4;
                g2.setColor(new Color(80, 90, 110));
                g2.setStroke(new BasicStroke(1f));
                g2.drawString(lbl + "ms", 2, y + 4);
            }

            g2.setColor(new Color(40, 46, 66));
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(pad, pad, pad, pad + gH);
            g2.drawLine(pad, pad + gH, pad + gW, pad + gH);

            if (pts.isEmpty()) { g2.dispose(); return; }

            Long[] arr = pts.toArray(new Long[0]);
            float step = (float) gW / Math.max(CAP - 1, 1);

            GeneralPath fill = new GeneralPath();
            GeneralPath line = new GeneralPath();
            boolean started = false;

            for (int i = 0; i < arr.length; i++) {
                if (arr[i] < 0) { started = false; continue; }
                float x = pad + i * step;
                float y = pad + gH - Math.min(gH, (float)(arr[i] * gH) / ceiling);
                if (!started) {
                    fill.moveTo(x, pad + gH);
                    fill.lineTo(x, y);
                    line.moveTo(x, y);
                    started = true;
                } else {
                    fill.lineTo(x, y);
                    line.lineTo(x, y);
                }
            }

            if (started) {
                fill.lineTo(pad + (arr.length - 1) * step, pad + gH);
                fill.closePath();
            }

            g2.setColor(new Color(0, 212, 170, 28));
            g2.fill(fill);
            g2.setColor(ACCENT);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(line);

            for (int i = 0; i < arr.length; i++) {
                float x = pad + i * step;
                if (arr[i] < 0) {
                    g2.setColor(ERR);
                    g2.fillOval((int)x - 4, pad + gH - 4, 8, 8);
                    g2.setColor(new Color(248, 81, 73, 150));
                    g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 9));
                    g2.drawString("T/O", (int)x - 8, pad + gH - 8);
                } else if (i == arr.length - 1) {
                    g2.setColor(ACCENT);
                    g2.fillOval((int)x - 4, (int)(pad + gH - Math.min(gH, (float)(arr[i] * gH) / ceiling)) - 4, 8, 8);
                }
            }

            g2.dispose();
        }
    }
}
