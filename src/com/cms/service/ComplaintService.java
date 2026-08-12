package com.cms.service;

import com.cms.enums.ComplaintStatus;
import com.cms.enums.Priority;
import com.cms.exceptions.ComplaintNotFoundException;
import com.cms.exceptions.InvalidStatusTransitionException;
import com.cms.model.Admin;
import com.cms.model.Category;
import com.cms.model.Complaint;
import com.cms.model.Student;

import java.util.*;

public class ComplaintService {
    private final List<Complaint> complaints = new ArrayList<>();
    private int nextId = 1001;

    public Complaint file(Student student, Category leaf, String title, String description, Priority priority) {
        if (!leaf.isLeaf()) {
            throw new IllegalArgumentException("Complaints can only be filed under a specific category, not a group like '" + leaf.getName() + "'");
        }
        Complaint c = new Complaint(nextId++, title, description, leaf, priority, student);
        complaints.add(c);
        return c;
    }

    public Complaint findById(int id) throws ComplaintNotFoundException {
        return complaints.stream().filter(c -> c.getId() == id).findFirst()
                .orElseThrow(() -> new ComplaintNotFoundException(String.valueOf(id)));
    }

    /** Complaints filed by a student, most urgent / oldest-waiting first. */
    public List<Complaint> forStudent(Student student) {
        List<Complaint> mine = new ArrayList<>();
        for (Complaint c : complaints) {
            if (c.getFiledBy().getId() == student.getId()) mine.add(c);
        }
        Collections.sort(mine);
        return mine;
    }

    /** Complaints routed to an admin's managed categories, most urgent first. */
    public List<Complaint> forAdmin(Admin admin) {
        List<Complaint> mine = new ArrayList<>();
        for (Complaint c : complaints) {
            if (admin.manages(c.getCategory().getId())) mine.add(c);
        }
        Collections.sort(mine);
        return mine;
    }

    public void updateStatus(int complaintId, ComplaintStatus target, String note)
            throws ComplaintNotFoundException, InvalidStatusTransitionException {
        findById(complaintId).changeStatus(target, note);
    }

    public void resolve(int complaintId, String notes)
            throws ComplaintNotFoundException, InvalidStatusTransitionException {
        findById(complaintId).resolve(notes);
    }

    /**
     * Escalation engine: scans all open/in-progress complaints and escalates
     * any that have exceeded their priority's SLA (minutes) since last update.
     * Call this periodically (e.g. from a background thread or a manual trigger).
     */
    public List<Complaint> runEscalationSweep() {
        List<Complaint> escalated = new ArrayList<>();
        for (Complaint c : complaints) {
            if (c.isOpenForEscalation() && c.minutesSinceUpdate() >= c.getPriority().getSlaMinutes()) {
                c.escalate();
                escalated.add(c);
            }
        }
        return escalated;
    }

    public List<Complaint> all() {
        List<Complaint> copy = new ArrayList<>(complaints);
        Collections.sort(copy);
        return copy;
    }
}
