package com.cms.service;

import com.cms.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryService {
    private final List<Category> all = new ArrayList<>();
    private int nextId = 1;
    private Category root;

    public CategoryService() {
        seed();
    }

    private Category add(String name, Category parent) {
        Category c = new Category(nextId++, name, parent);
        all.add(c);
        return c;
    }

    private void seed() {
        root = add("COMPLAINT", null);

        Category college = add("COLLEGE", root);
        add("CANTEEN", college).setHandlerEmail("canteen.admin@college.edu");
        add("STORE", college).setHandlerEmail("store.admin@college.edu");
        add("MAINTENANCE", college).setHandlerEmail("maintenance.admin@college.edu");
        add("TRANSPORT", college).setHandlerEmail("transport.admin@college.edu");
        add("ADMINISTRATIVE", college).setHandlerEmail("office.admin@college.edu");

        Category faculty = add("FACULTY", college);
        Category cse = add("CSE", faculty);
        add("AI", cse).setHandlerEmail("cse.ai@college.edu");
        add("Cyber Security", cse).setHandlerEmail("cse.cyber@college.edu");
        add("CS (Core)", cse).setHandlerEmail("cse.core@college.edu");
        add("Design", cse).setHandlerEmail("cse.design@college.edu");
        add("CS Business Systems", cse).setHandlerEmail("cse.business@college.edu");
        add("EEE", faculty).setHandlerEmail("eee.hod@college.edu");
        add("ECE", faculty).setHandlerEmail("ece.hod@college.edu");
        add("MECH", faculty).setHandlerEmail("mech.hod@college.edu");
        add("CE", faculty).setHandlerEmail("ce.hod@college.edu");

        Category others = add("OTHERS", college);
        add("INDIVIDUAL COMPLAINT", others).setHandlerEmail("office.admin@college.edu");
        add("LIBRARY", others).setHandlerEmail("library.admin@college.edu");
        add("EXAMINATION SECTION", others).setHandlerEmail("exam.admin@college.edu");
        add("OTHER", others).setHandlerEmail("office.admin@college.edu");

        Category hostel = add("HOSTEL", root);
        add("ROOM / MAINTENANCE", hostel).setHandlerEmail("hostel.warden@college.edu");
        add("MESS / FOOD", hostel).setHandlerEmail("hostel.warden@college.edu");
        add("WARDEN / DISCIPLINE", hostel).setHandlerEmail("hostel.warden@college.edu");
    }

    public Category getRoot() { return root; }
    public List<Category> getTopLevel() { return root.getChildren(); }
    public List<Category> getAll() { return all; }

    public Category findById(int id) {
        return all.stream().filter(c -> c.getId() == id).findFirst().orElse(null);
    }

    public List<Category> getLeaves() {
        List<Category> leaves = new ArrayList<>();
        for (Category c : all) {
            if (c.isLeaf()) leaves.add(c);
        }
        return leaves;
    }
}
