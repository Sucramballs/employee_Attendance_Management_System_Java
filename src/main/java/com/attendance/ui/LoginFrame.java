package com.attendance.ui;
 
import com.attendance.dao.UserDAO;
import com.attendance.model.User;
import com.attendance.util.SessionManager;
 
import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
 
/**
 * Login window. Validates input, authenticates through UserDAO,
 * stores the user in SessionManager, then opens the next screen.
 */
public class LoginFrame extends JFrame {
 
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JButton loginBtn = new JButton("LOGIN");
    private final JButton clearBtn = new JButton("CLEAR");
    private final JButton exitBtn = new JButton("EXIT");
 
    private final UserDAO userDAO = new UserDAO();
 
    public LoginFrame() {
        setTitle("Employee Attendance Management System - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        buildUI();
        wireEvents();
        pack();
        setLocationRelativeTo(null); // center on screen
    }
 
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 15));
        root.setBorder(BorderFactory.createEmptyBorder(25, 35, 25, 35));
 
        // Header
        JLabel title = new JLabel("Attendance System", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        JLabel subtitle = new JLabel("Please sign in to continue", SwingConstants.CENTER);
        subtitle.setForeground(Color.GRAY);
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.add(title);
        header.add(subtitle);
        root.add(header, BorderLayout.NORTH);
 
        // Form
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 4, 6, 4);
        gc.anchor = GridBagConstraints.WEST;
 
        gc.gridx = 0; gc.gridy = 0;
        form.add(new JLabel("Username:"), gc);
        gc.gridx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
        form.add(usernameField, gc);
 
        gc.gridx = 0; gc.gridy = 1; gc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Password:"), gc);
        gc.gridx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
        form.add(passwordField, gc);
 
        root.add(form, BorderLayout.CENTER);
 
        // Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttons.add(loginBtn);
        buttons.add(clearBtn);
        buttons.add(exitBtn);
        root.add(buttons, BorderLayout.SOUTH);
 
        setContentPane(root);
        getRootPane().setDefaultButton(loginBtn); // Enter key = Login
    }
 
    private void wireEvents() {
        loginBtn.addActionListener(e -> doLogin());
        clearBtn.addActionListener(e -> clearFields());
        exitBtn.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to exit?", "Exit",
                    JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        });
    }
 
    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
 
        // Input validation
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both username and password.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            (username.isEmpty() ? usernameField : passwordField).requestFocus();
            return;
        }
 
        try {
            User user = userDAO.authenticate(username, password);
 
            if (user == null) {
                JOptionPane.showMessageDialog(this,
                        "Invalid username or password, or the account is inactive.",
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
                passwordField.setText("");
                passwordField.requestFocus();
                return;
            }
 
            SessionManager.login(user);
            openNextScreen(user);
 
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not reach the database:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
 
  private void openNextScreen(User user) {
    new DashboardFrame().setVisible(true);
    dispose();
}
 
    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        usernameField.requestFocus();
    }
}
