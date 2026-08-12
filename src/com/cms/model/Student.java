package com.cms.model;

import com.cms.enums.Role;

public class Student extends User {
    private final String department;

    public Student(int id, String name, String email, String password, String department) {
        super(id, name, email, password);
        this.department = department;
    }

    public String getDepartment() { return department; }

    @Override
    public Role getRole() { return Role.CUSTOMER; }
}
