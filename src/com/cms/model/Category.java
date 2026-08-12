package com.cms.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A node in the complaint category tree (e.g. COLLEGE -> FACULTY -> CSE -> AI).
 * Self-referencing structure so the tree can be arbitrarily deep.
 * Only LEAF categories (no children) can have complaints filed against them.
 */
public class Category {
    private final int id;
    private final String name;
    private final Category parent;
    private final List<Category> children = new ArrayList<>();
    private String handlerEmail; // admin account that resolves complaints filed here (leaf only)

    public Category(int id, String name, Category parent) {
        this.id = id;
        this.name = name;
        this.parent = parent;
        if (parent != null) {
            parent.children.add(this);
        }
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public Category getParent() { return parent; }
    public List<Category> getChildren() { return children; }
    public boolean isLeaf() { return children.isEmpty(); }
    public String getHandlerEmail() { return handlerEmail; }
    public void setHandlerEmail(String handlerEmail) { this.handlerEmail = handlerEmail; }

    /** Full breadcrumb path, e.g. "COLLEGE > FACULTY > CSE > AI". */
    public String getPath() {
        return (parent == null ? "" : parent.getPath() + " > ") + name;
    }

    @Override
    public String toString() {
        return name;
    }
}
