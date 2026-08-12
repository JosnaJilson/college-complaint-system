package com.cms.service;

import com.cms.model.Admin;
import com.cms.model.Category;
import com.cms.model.Student;
import com.cms.model.User;

import java.util.*;

/**
 * Handles signup/login. Students must register with the college email
 * domain. Sessions are a simple in-memory token -> user map (cookie-based).
 */
public class AuthService {
    // Change this to your real college domain.
    public static final String ALLOWED_STUDENT_DOMAIN = "@college.edu";

    private final Map<String, User> usersByEmail = new HashMap<>();
    private final Map<String, User> sessions = new HashMap<>(); // token -> user
    private int nextId = 1;

    public AuthService(CategoryService categoryService) {
        seedAdmins(categoryService);
    }

    private void seedAdmins(CategoryService cs) {
        // One admin account per distinct handler email found in the category tree.
        Map<String, Admin> byEmail = new LinkedHashMap<>();
        for (Category leaf : cs.getLeaves()) {
            String email = leaf.getHandlerEmail();
            if (email == null) continue;
            Admin admin = byEmail.computeIfAbsent(email, e ->
                    new Admin(nextId++, deriveTitle(leaf), e, "admin123", deriveTitle(leaf)));
            admin.addManagedCategory(leaf.getId());
        }
        byEmail.values().forEach(a -> usersByEmail.put(a.getEmail(), a));
    }

    private String deriveTitle(Category leaf) {
        Category p = leaf.getParent();
        if (p != null && p.getName().equals("CSE")) return leaf.getName() + " Coordinator";
        if (p != null && p.getName().equals("HOSTEL")) return "Hostel Warden";
        if (p != null && p.getName().equals("FACULTY")) return leaf.getName() + " HOD";
        return leaf.getName() + " In-charge";
    }

    public String signupStudent(String name, String email, String password, String department) {
        if (!email.toLowerCase().endsWith(ALLOWED_STUDENT_DOMAIN)) {
            throw new IllegalArgumentException("Please use your college email (" + ALLOWED_STUDENT_DOMAIN + ")");
        }
        if (usersByEmail.containsKey(email)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }
        Student s = new Student(nextId++, name, email, password, department);
        usersByEmail.put(email, s);
        return createSession(s);
    }

    public String login(String email, String password) {
        User u = usersByEmail.get(email);
        if (u == null || !u.checkPassword(password)) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        return createSession(u);
    }

    private String createSession(User u) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, u);
        return token;
    }

    public User getUserForToken(String token) {
        return token == null ? null : sessions.get(token);
    }

    public void logout(String token) {
        if (token != null) sessions.remove(token);
    }

    public List<Admin> getAllAdmins() {
        List<Admin> admins = new ArrayList<>();
        for (User u : usersByEmail.values()) {
            if (u instanceof Admin) admins.add((Admin) u);
        }
        return admins;
    }
}
