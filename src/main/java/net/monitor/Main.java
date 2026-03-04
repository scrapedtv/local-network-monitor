package net.monitor;

import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        applyTheme();
        SwingUtilities.invokeLater(() -> new MonitorUI().setVisible(true));
    }

    static void applyTheme() {
        try {
            for (UIManager.LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(laf.getName())) {
                    UIManager.setLookAndFeel(laf.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        Color bg     = new Color(10,  14,  26);
        Color panel  = new Color(16,  20,  35);
        Color card   = new Color(22,  27,  45);
        Color accent = new Color(0,  212, 170);
        Color text   = new Color(230, 237, 243);
        Color muted  = new Color(139, 148, 158);
        Color warn   = new Color(240, 165,   0);
        Color err    = new Color(248,  81,  73);
        Color ok     = new Color( 63, 185,  80);

        UIManager.put("control",                    panel);
        UIManager.put("info",                       card);
        UIManager.put("nimbusBase",                 bg);
        UIManager.put("nimbusAlertYellow",          warn);
        UIManager.put("nimbusDisabledText",         muted);
        UIManager.put("nimbusFocus",                accent);
        UIManager.put("nimbusGreen",                ok);
        UIManager.put("nimbusInfoBlue",             accent);
        UIManager.put("nimbusLightBackground",      card);
        UIManager.put("nimbusOrange",               warn);
        UIManager.put("nimbusRed",                  err);
        UIManager.put("nimbusSelectedText",         bg);
        UIManager.put("nimbusSelectionBackground",  accent);
        UIManager.put("text",                       text);
        UIManager.put("ScrollBar.thumbHighlight",   panel);
        UIManager.put("ScrollBar.thumb",            new Color(48, 54, 61));
        UIManager.put("ScrollBar.track",            panel);
    }
}
