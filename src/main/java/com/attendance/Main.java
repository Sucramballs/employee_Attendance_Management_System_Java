package com.attendance;

import com.attendance.ui.LoginFrame;
import com.attendance.util.AppTheme;
import com.attendance.util.DatabaseInitializer;

import javax.swing.JOptionPane;
import java.awt.EventQueue;

public class Main {

    public static void main(String[] args) {
        AppTheme.install();                    // 1. look and feel first

        try {
            DatabaseInitializer.initialize();  // 2. create tables on first run
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Database setup failed:\n" + e.getMessage(),
                    "Startup Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        EventQueue.invokeLater(() -> new LoginFrame().setVisible(true)); // 3. login
    }
}