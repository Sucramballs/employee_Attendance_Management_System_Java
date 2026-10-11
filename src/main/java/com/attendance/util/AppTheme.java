package com.attendance.util;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.Collections;

/**
 * One place to change the look of the whole system:
 *   - the system name
 *   - the color scheme
 *   - the logo
 *
 * Call AppTheme.install() once, FIRST THING in Main.main(), before any window is created.
 */
public final class AppTheme {

    // ---------------------------------------------------------- NAME
    public static final String APP_NAME = "TUP EAMS";
    public static final String APP_TAGLINE = "Employee Attendance Management System";
    public static final String UNIVERSITY = "Technological University of the Philippines - Taguig";

    // ---------------------------------------------------------- LOGO
    // Put your logo at: src/main/resources/images/logo.png
    private static final String LOGO_PATH = "/images/logo.png";

    // -------------------------------------------------------- COLORS
    // Maroon / red / white / beige. Change these to re-color the whole system.
    public static final String ACCENT_HEX = "#7B1E2B";            // buttons, selections, focus rings
    public static final Color MAROON = new Color(123, 30, 43);    // #7B1E2B  main brand color
    public static final Color RED    = new Color(198, 40, 40);    // #C62828  highlight
    public static final Color BEIGE  = new Color(245, 239, 230);  // #F5EFE6  window background
    public static final Color TAN    = new Color(166, 124, 82);   // #A67C52  warm secondary
    public static final Color BROWN  = new Color(93, 64, 55);     // #5D4037  dark neutral

    // the four dashboard cards
    public static final Color PRIMARY   = MAROON; // card 1
    public static final Color SECONDARY = RED;    // card 2
    public static final Color SUCCESS   = TAN;    // card 3
    public static final Color WARNING   = BROWN;  // card 4

    private static Image logoImage;
    private static boolean logoTried = false;

    private AppTheme() {}

    /** Sets the look and feel. Must run before any window is created. */
    public static void install() {
        // accent color must be set BEFORE setup()
        FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", ACCENT_HEX));
        FlatLightLaf.setup(); // for a dark theme use: com.formdev.flatlaf.FlatDarkLaf.setup();

        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 8);

        // beige windows, white inputs and tables
        UIManager.put("Panel.background", BEIGE);
        UIManager.put("OptionPane.background", BEIGE);

        // maroon table headers and selected rows
        UIManager.put("TableHeader.background", MAROON);
        UIManager.put("TableHeader.foreground", Color.WHITE);
        UIManager.put("Table.selectionBackground", MAROON);
        UIManager.put("Table.selectionForeground", Color.WHITE);

        // maroon default buttons (LOGIN, SAVE...) and form section titles
        UIManager.put("Button.default.background", MAROON);
        UIManager.put("Button.default.foreground", Color.WHITE);
        UIManager.put("TitledBorder.titleColor", MAROON);
    }

    /** Sets the window title ("System Name - Screen") and the window/taskbar icon. */
    public static void brand(JFrame frame, String screenName) {
        frame.setTitle(APP_NAME + " - " + screenName);
        Image icon = loadLogo();
        if (icon != null) {
            frame.setIconImage(icon);
        }
    }

    /** Logo scaled to the given height, or null if no logo file exists (safe to pass to JLabel). */
    public static ImageIcon logoIcon(int height) {
        Image img = loadLogo();
        if (img == null) return null;
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        if (w <= 0 || h <= 0) return null;
        int newW = Math.max(1, w * height / h);
        return new ImageIcon(img.getScaledInstance(newW, height, Image.SCALE_SMOOTH));
    }

    /** A centered label holding the logo, or null if no logo file exists. */
    public static JLabel logoLabel(int height) {
        ImageIcon icon = logoIcon(height);
        return (icon == null) ? null : new JLabel(icon, SwingConstants.CENTER);
    }

    private static Image loadLogo() {
        if (!logoTried) {
            logoTried = true;
            URL url = AppTheme.class.getResource(LOGO_PATH);
            if (url != null) {
                logoImage = new ImageIcon(url).getImage();
            }
        }
        return logoImage;
    }
}
