package com.attendance.util;
 
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
 
/**
 * Creates the Derby tables and loads the seed data the first time the app runs.
 * If the USERS table already exists, nothing happens.
 *
 * Scripts are read from the classpath:
 *   src/main/resources/db/migration/V1__derby_schema.sql
 *   src/main/resources/db/migration/V2__derby_seed.sql
 */
public class DatabaseInitializer {
 
    private static final String SCHEMA_SCRIPT = "db/migration/V1__derby_schema.sql";
    private static final String SEED_SCRIPT = "db/migration/V2__derby_seed.sql";
 
    private DatabaseInitializer() {}
 
    /** Call once at startup, before showing any window. */
    public static void initialize() throws SQLException, IOException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (tableExists(conn, "USERS")) {
                return; // database is already set up
            }
 
            conn.setAutoCommit(false); // all-or-nothing: no half-built database
            try {
                runScript(conn, SCHEMA_SCRIPT);
                runScript(conn, SEED_SCRIPT);
                conn.commit();
                System.out.println("Database created and seeded.");
            } catch (SQLException | IOException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }
 
    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        try (ResultSet rs = conn.getMetaData()
                .getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }
 
    private static void runScript(Connection conn, String resourcePath)
            throws SQLException, IOException {
        String script = readResource(resourcePath);
 
        try (Statement st = conn.createStatement()) {
            for (String sql : splitStatements(script)) {
                try {
                    st.execute(sql);
                } catch (SQLException ex) {
                    String preview = sql.length() > 80 ? sql.substring(0, 80) + "..." : sql;
                    throw new SQLException("Error in " + resourcePath + " near: "
                            + preview.replace('\n', ' ') + " -> " + ex.getMessage(), ex);
                }
            }
        }
    }
 
    private static String readResource(String path) throws IOException {
        try (InputStream in = DatabaseInitializer.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("SQL script not found on classpath: " + path);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            String text = new String(out.toByteArray(), StandardCharsets.UTF_8);
            if (text.startsWith("\uFEFF")) {
                text = text.substring(1); // strip BOM if the file has one
            }
            return text;
        }
    }
 
    /**
     * Splits a script into individual statements on ';'.
     * Ignores -- line comments, block comments, and semicolons inside 'quotes'.
     */
    private static List<String> splitStatements(String script) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        int n = script.length();
 
        for (int i = 0; i < n; i++) {
            char c = script.charAt(i);
            char next = (i + 1 < n) ? script.charAt(i + 1) : '\0';
 
            if (inQuote) {
                cur.append(c);
                if (c == '\'') {
                    if (next == '\'') { // escaped quote ''
                        cur.append(next);
                        i++;
                    } else {
                        inQuote = false;
                    }
                }
                continue;
            }
 
            if (c == '-' && next == '-') {          // line comment
                while (i < n && script.charAt(i) != '\n') i++;
                cur.append('\n');
                continue;
            }
            if (c == '/' && next == '*') {          // block comment
                int end = script.indexOf("*/", i + 2);
                i = (end == -1) ? n : end + 1;
                cur.append(' ');
                continue;
            }
            if (c == '\'') {
                inQuote = true;
                cur.append(c);
                continue;
            }
            if (c == ';') {
                addIfNotEmpty(out, cur);
                cur.setLength(0);
                continue;
            }
            cur.append(c);
        }
        addIfNotEmpty(out, cur);
        return out;
    }
 
    private static void addIfNotEmpty(List<String> list, StringBuilder sb) {
        String s = sb.toString().trim();
        if (!s.isEmpty()) {
            list.add(s);
        }
    }
}
 
