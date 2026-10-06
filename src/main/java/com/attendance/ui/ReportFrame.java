package com.attendance.ui;

import com.attendance.dao.AttendanceDAO;
import com.attendance.dao.DepartmentDAO;
import com.attendance.dao.EmployeeDAO;
import com.attendance.dao.LeaveDAO;
import com.attendance.model.AttendanceLog;
import com.attendance.model.Department;
import com.attendance.model.Employee;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Attendance summary report: one row per employee for a date range, with
 * present / on-time / late / half-day / undertime counts and approved leave days.
 *
 * Built only from existing DAO methods (no SQL view needed). Undertime is
 * derived the same way AttendanceFrame shows it: an ON_TIME record whose
 * clock-out is before the shift end.
 */
public class ReportFrame extends JFrame {

    private static final String ALL_DEPARTMENTS = "All Departments";
    private static final String[] COLUMNS = {
        "Emp ID", "Employee", "Department", "Days Present", "On Time",
        "Late", "Half Day", "Undertime", "Approved Leave Days"
    };

    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();
    private final LeaveDAO leaveDAO = new LeaveDAO();

    private final JTextField startField =
            new JTextField(LocalDate.now().withDayOfMonth(1).toString(), 10);
    private final JTextField endField = new JTextField(LocalDate.now().toString(), 10);
    private final JComboBox<Object> deptCombo = new JComboBox<>();
    private final JButton generateBtn = new JButton("GENERATE");
    private final JButton thisMonthBtn = new JButton("This Month");
    private final JButton closeBtn = new JButton("CLOSE");
    private final JLabel summaryLabel = new JLabel(" ");

    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
        @Override public Class<?> getColumnClass(int c) {
            return (c == 1 || c == 2) ? String.class : Integer.class;
        }
    };
    private final JTable table = new JTable(tableModel);

    public ReportFrame() {
        setTitle("Attendance Summary Report");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1050, 620);
        setMinimumSize(new Dimension(900, 500));

        buildUI();
        loadDepartments();
        wireEvents();
        generateReport();
        setLocationRelativeTo(null);
    }

    // ---------------------------------------------------------------- UI

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        filters.setBorder(BorderFactory.createTitledBorder("Report Filters"));
        filters.add(new JLabel("From (yyyy-MM-dd):"));
        filters.add(startField);
        filters.add(new JLabel("To:"));
        filters.add(endField);
        filters.add(thisMonthBtn);
        filters.add(Box.createHorizontalStrut(12));
        filters.add(new JLabel("Department:"));
        filters.add(deptCombo);
        filters.add(generateBtn);
        root.add(filters, BorderLayout.NORTH);

        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(170);
        table.getColumnModel().getColumn(2).setPreferredWidth(170);
        root.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.add(summaryLabel, BorderLayout.CENTER);
        JPanel btn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btn.add(closeBtn);
        south.add(btn, BorderLayout.EAST);
        root.add(south, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void wireEvents() {
        generateBtn.addActionListener(e -> generateReport());
        deptCombo.addActionListener(e -> generateReport());
        thisMonthBtn.addActionListener(e -> {
            startField.setText(LocalDate.now().withDayOfMonth(1).toString());
            endField.setText(LocalDate.now().toString());
            generateReport();
        });
        closeBtn.addActionListener(e -> dispose());
    }

    private void loadDepartments() {
        try {
            deptCombo.addItem(ALL_DEPARTMENTS);
            for (Department d : departmentDAO.getAllDepartments()) {
                deptCombo.addItem(d);
            }
        } catch (SQLException ex) {
            showDbError(ex);
        }
    }

    // ------------------------------------------------------------ report

    private void generateReport() {
        LocalDate start;
        LocalDate end;
        try {
            start = LocalDate.parse(startField.getText().trim());
        } catch (DateTimeParseException ex) {
            warn("The 'From' date must look like " + LocalDate.now().withDayOfMonth(1) + " (yyyy-MM-dd).");
            startField.requestFocus();
            return;
        }
        try {
            end = LocalDate.parse(endField.getText().trim());
        } catch (DateTimeParseException ex) {
            warn("The 'To' date must look like " + LocalDate.now() + " (yyyy-MM-dd).");
            endField.requestFocus();
            return;
        }
        if (end.isBefore(start)) {
            warn("The 'To' date cannot be before the 'From' date.");
            endField.requestFocus();
            return;
        }

        Object deptSel = deptCombo.getSelectedItem();
        Integer deptFilter = (deptSel instanceof Department)
                ? ((Department) deptSel).getDepartmentId() : null;

        try {
            List<Employee> employees = employeeDAO.getAllEmployees();
            Map<Integer, Employee> empMap = new HashMap<>();
            for (Employee e : employees) {
                empMap.put(e.getEmployeeId(), e);
            }

            Map<Integer, String> deptNames = new HashMap<>();
            for (Department d : departmentDAO.getAllDepartments()) {
                deptNames.put(d.getDepartmentId(), d.getDepartmentName());
            }

            // counts per employee: [present, onTime, late, halfDay, undertime, leaveDays]
            Map<Integer, int[]> counts = new HashMap<>();
            for (AttendanceLog log : attendanceDAO.getLogsByDateRange(start.toString(), end.toString())) {
                Employee emp = empMap.get(log.getEmployeeId());
                if (emp == null) continue;

                String status = log.getStatus();
                if ("ON_TIME".equals(status) && log.getTimeOut() != null) {
                    try {
                        if (LocalTime.parse(log.getTimeOut()).isBefore(LocalTime.parse(emp.getShiftEnd()))) {
                            status = "UNDERTIME";
                        }
                    } catch (DateTimeParseException ignored) {
                        // keep the stored status if a time can't be read
                    }
                }

                int[] c = counts.computeIfAbsent(emp.getEmployeeId(), k -> new int[6]);
                c[0]++;
                if ("ON_TIME".equals(status)) c[1]++;
                else if ("LATE".equals(status)) c[2]++;
                else if ("HALF_DAY".equals(status)) c[3]++;
                else if ("UNDERTIME".equals(status)) c[4]++;
            }

            // approved leave days that fall inside the range
            for (Object[] l : leaveDAO.getAllLeavesWithDetails()) {
                if (!"APPROVED".equals(l[7])) continue;
                LocalDate ls = LocalDate.parse((String) l[4]);
                LocalDate le = LocalDate.parse((String) l[5]);
                LocalDate from = ls.isAfter(start) ? ls : start;
                LocalDate to = le.isBefore(end) ? le : end;
                if (to.isBefore(from)) continue;
                int[] c = counts.computeIfAbsent((Integer) l[1], k -> new int[6]);
                c[5] += (int) (ChronoUnit.DAYS.between(from, to) + 1);
            }

            tableModel.setRowCount(0);
            int[] totals = new int[6];
            int shown = 0;
            for (Employee e : employees) {
                if (deptFilter != null && e.getDepartmentId() != deptFilter) continue;
                int[] c = counts.getOrDefault(e.getEmployeeId(), new int[6]);
                // hide inactive staff who have nothing in this range
                if (!"ACTIVE".equals(e.getStatus()) && c[0] == 0 && c[5] == 0) continue;

                tableModel.addRow(new Object[]{
                    e.getEmployeeId(),
                    e.getFirstName() + " " + e.getLastName(),
                    deptNames.getOrDefault(e.getDepartmentId(), "Unknown"),
                    c[0], c[1], c[2], c[3], c[4], c[5]
                });
                for (int i = 0; i < 6; i++) totals[i] += c[i];
                shown++;
            }

            summaryLabel.setText("  " + shown + " employee(s)  |  " + start + " to " + end
                    + "  |  Present days: " + totals[0] + "   On time: " + totals[1]
                    + "   Late: " + totals[2] + "   Half day: " + totals[3]
                    + "   Undertime: " + totals[4] + "   Approved leave days: " + totals[5]);
        } catch (SQLException ex) {
            showDbError(ex);
        } catch (DateTimeParseException ex) {
            showDbError(new SQLException("A stored leave date could not be read: " + ex.getMessage()));
        }
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "Invalid Input", JOptionPane.WARNING_MESSAGE);
    }

    private void showDbError(SQLException ex) {
        JOptionPane.showMessageDialog(this,
                "Database error:\n" + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        ex.printStackTrace();
    }
}
