package com.cms.model;

import com.cms.enums.Role;
import java.util.HashSet;
import java.util.Set;

/**
 * An Admin is a handler tied to one or more leaf categories
 * (e.g. the CSE-AI coordinator, or the Hostel Warden).
 */
public class Admin extends User {
    private final String title; // e.g. "Hostel Warden", "CSE HOD"
    private final Set<Integer> managedCategoryIds = new HashSet<>();

    public Admin(int id, String name, String email, String password, String title) {
        super(id, name, email, password);
        this.title = title;
    }

    public String getTitle() { return title; }
    public void addManagedCategory(int categoryId) { managedCategoryIds.add(categoryId); }
    public boolean manages(int categoryId) { return managedCategoryIds.contains(categoryId); }

    @Override
    public Role getRole() { return Role.ADMIN; }
}
