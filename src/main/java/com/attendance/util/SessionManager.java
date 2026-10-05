package com.attendance.util;

import com.attendance.model.User;

public class SessionManager {
    private static User currentUser;

    private SessionManager() {}

    public static void login(User user) { currentUser = user; }
    public static void logout() { currentUser = null; }
    public static User getCurrentUser() { return currentUser; }
    public static boolean isLoggedIn() { return currentUser != null; }

    public static boolean isAdmin() {
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole());
    }
    public static boolean isManager() {
        return currentUser != null && "MANAGER".equalsIgnoreCase(currentUser.getRole());
    }
    public static boolean isStaff() {
        return currentUser != null && "STAFF".equalsIgnoreCase(currentUser.getRole());
    }
}