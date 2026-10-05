/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.attendance;

/**
 *
 * @author Aaron
 */

// Main Entry Point for the Project


  

    public class Main {

    public static void main(String[] args) {
        try {
            com.attendance.util.DatabaseInitializer.initialize();
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null,
                    "Database setup failed:\n" + e.getMessage(),
                    "Startup Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }
        java.awt.EventQueue.invokeLater(() ->
                new com.attendance.ui.LoginFrame().setVisible(true));
    }
}