package com.attendance.ui;

import com.attendance.dao.AttendanceDAO;
import com.attendance.dao.DepartmentDAO;
import com.attendance.dao.EmployeeDAO;
import com.attendance.model.User;
import com.attendance.util.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Landing screen after login: metric cards, menu bar, and navigation buttons.
 */
public class DashboardFrame extends JFrame {

    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();

    private final JLabel totalEmployeesValue = new JLabel("0", SwingConstants.CENTER);
    private final JLabel departmentsValue = new JLabel("0", SwingConstants.CENTER);
    private final JLabel presentTodayValue = new JLabel("-", SwingConstants.CENTER);
    private final JLabel pendingLeavesValue = new JLabel("-", SwingConstants.CENTER);

    private EmployeeFrame employeeFrame;     // reused so only one copy opens
    private AttendanceFrame attendanceFrame; // same for attendance

    public DashboardFrame() {
        setTitle("Employee Attendance Management System - Dashboard");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(950, 520);
        setMinimumSize(new Dimension(800, 460));

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { confirmExit(); }
        });
        // refresh the numbers whenever the user comes back to this window
        addWindowFocusListener(new WindowAdapter() {
            @Override public void windowGainedFocus(WindowEvent e) { refreshMetrics(); }
        });

        setJMenuBar(buildMenuBar());
        setContentPane(buildContent());
        refreshMetrics();
        setLocationRelativeTo(null);
    }

    // ---------------------------------------------------------------- UI

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        file.add(menuItem("Logout", this::logout));
        file.addSeparator();
        file.add(menuItem("Exit", this::confirmExit));

        JMenu manage = new JMenu("Manage");
        manage.add(menuItem("Employees", this::openEmployees));
        manage.add(menuItem("Attendance", this::openAttendance));
        manage.add(menuItem("Leave Requests", () -> comingSoon("Leave")));
        manage.add(menuItem("Reports", () -> comingSoon("Reports")));

        bar.add(file);
        bar.add(manage);
        return bar;
    }

    private JMenuItem menuItem(String text, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(e -> action.run());
        return item;
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 20));
        root.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // header
        JLabel title = new JLabel("Dashboard");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        User u = SessionManager.getCurrentUser();
        JLabel who = new JLabel(u == null ? "Not signed in"
                : "Signed in as " + u.getFullName() + " (" + u.getRole() + ")");
        who.setForeground(Color.GRAY);
        JPanel header = new JPanel(new BorderLayout());
        header.add(title, BorderLayout.WEST);
        header.add(who, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // metric cards
        JPanel cards = new JPanel(new GridLayout(1, 4, 15, 0));
        cards.add(createCard("Total Employees", totalEmployeesValue, new Color(41, 98, 255)));
        cards.add(createCard("Departments", departmentsValue, new Color(0, 150, 136)));
        cards.add(createCard("Present Today", presentTodayValue, new Color(46, 160, 67)));
        cards.add(createCard("Pending Leaves", pendingLeavesValue, new Color(230, 126, 34)));

        // navigation buttons
        JPanel nav = new JPanel(new GridLayout(1, 4, 15, 0));
        nav.setBorder(BorderFactory.createTitledBorder("Quick Access"));
        nav.add(navButton("Employees", this::openEmployees));
        nav.add(navButton("Attendance", this::openAttendance));
        nav.add(navButton("Leave Requests", () -> comingSoon("Leave")));
        nav.add(navButton("Reports", () -> comingSoon("Reports")));

        JPanel navWrap = new JPanel(new BorderLayout());
        navWrap.add(nav, BorderLayout.NORTH);

        JPanel logoutRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        JButton logoutBtn = new JButton("LOGOUT");
        logoutBtn.addActionListener(e -> logout());
        logoutRow.add(logoutBtn);

        JPanel center = new JPanel(new BorderLayout(0, 20));
        center.add(cards, BorderLayout.NORTH);
        center.add(navWrap, BorderLayout.CENTER);

        root.add(center, BorderLayout.CENTER);
        root.add(logoutRow, BorderLayout.SOUTH);
        return root;
    }

    private JPanel createCard(String titleText, JLabel value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(5, 0, 0, 0, accent),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(220, 220, 220)),
                        BorderFactory.createEmptyBorder(14, 16, 14, 16))));

        JLabel t = new JLabel(titleText, SwingConstants.CENTER);
        t.setForeground(Color.GRAY);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 13f));

        value.setFont(new Font("SansSerif", Font.BOLD, 38));
        value.setForeground(accent.darker());

        card.add(t, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JButton navButton(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
        b.setPreferredSize(new Dimension(180, 70));
        b.addActionListener(e -> action.run());
        return b;
    }

    // ----------------------------------------------------------- actions

    private void refreshMetrics() {
        try {
            totalEmployeesValue.setText(String.valueOf(employeeDAO.getAllEmployees().size()));
            departmentsValue.setText(String.valueOf(departmentDAO.getAllDepartments().size()));
            presentTodayValue.setText(String.valueOf(
                    attendanceDAO.getPresentEmployeeCount(LocalDate.now().toString())));
            // TODO: Pending Leaves -> LeaveDAO (after LeaveDAO is reviewed)
        } catch (SQLException ex) {
            totalEmployeesValue.setText("!");
            departmentsValue.setText("!");
            ex.printStackTrace();
        }
    }

    private void openEmployees() {
        if (employeeFrame == null || !employeeFrame.isDisplayable()) {
            employeeFrame = new EmployeeFrame();
            employeeFrame.setVisible(true);
        } else {
            employeeFrame.setState(Frame.NORMAL);
            employeeFrame.toFront();
        }
    }

    private void openAttendance() {
        if (attendanceFrame == null || !attendanceFrame.isDisplayable()) {
            attendanceFrame = new AttendanceFrame();
            attendanceFrame.setVisible(true);
        } else {
            attendanceFrame.setState(Frame.NORMAL);
            attendanceFrame.toFront();
        }
    }

    private void comingSoon(String screen) {
        JOptionPane.showMessageDialog(this,
                "The " + screen + " screen has not been built yet.",
                "Coming Soon", JOptionPane.INFORMATION_MESSAGE);
    }

    private void logout() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Log out of your account?", "Logout", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;

        SessionManager.logout();
        if (employeeFrame != null) employeeFrame.dispose();
        if (attendanceFrame != null) attendanceFrame.dispose();
        dispose();
        new LoginFrame().setVisible(true);
    }

    private void confirmExit() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to exit?", "Exit", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }
}
