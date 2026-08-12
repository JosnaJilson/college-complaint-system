package com.cms.model;

import com.cms.enums.Role;

/**
 * Abstract base for anyone who can log in. Encapsulates common identity
 * fields; subclasses (Student, Admin) add role-specific behaviour,
 * demonstrating inheritance + polymorphism.
 */
public abstract class User {
    private final int id;
    private final String name;
    private final String email;
    private final String password; // plain text for demo simplicity only

    protected User(int id, String name, String email, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public boolean checkPassword(String candidate) { return password.equals(candidate); }

    public abstract Role getRole();
}
