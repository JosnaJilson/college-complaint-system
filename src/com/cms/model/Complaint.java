package com.cms.model;

import com.cms.enums.ComplaintStatus;
import com.cms.enums.Priority;
import com.cms.exceptions.InvalidStatusTransitionException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Core entity of the system. Encapsulates its own status transitions
 * (nothing outside this class can set status to something illegal)
 * and implements Comparable so complaint lists can be sorted by
 * priority (most urgent first), then by how long they've waited.
 */
public class Complaint implements Comparable<Complaint> {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final int id;
    private final String title;
    private final String description;
    private final Category category;
    private Priority priority;
    private ComplaintStatus status;
    private final Student filedBy;
    private final LocalDateTime dateFiled;
    private LocalDateTime lastUpdated;
    private String resolutionNotes;
    private int escalationLevel = 0;
    private final List<String> history = new ArrayList<>();

    public Complaint(int id, String title, String description, Category category,
                      Priority priority, Student filedBy) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.filedBy = filedBy;
        this.status = ComplaintStatus.OPEN;
        this.dateFiled = LocalDateTime.now();
        this.lastUpdated = this.dateFiled;
        addHistory("Filed by " + filedBy.getName() + " under " + category.getPath() + " [" + priority + "]");
    }

    public void changeStatus(ComplaintStatus target, String note) throws InvalidStatusTransitionException {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(status, target);
        }
        this.status = target;
        this.lastUpdated = LocalDateTime.now();
        addHistory("Status -> " + target + (note == null || note.isBlank() ? "" : " (" + note + ")"));
    }

    /** Called by the escalation engine; bypasses normal transition rules since ESCALATED is always reachable from OPEN/IN_PROGRESS. */
    public void escalate() {
        this.priority = this.priority.escalateUp();
        this.escalationLevel++;
        this.status = ComplaintStatus.ESCALATED;
        this.lastUpdated = LocalDateTime.now();
        addHistory("Auto-escalated (level " + escalationLevel + "), priority now " + priority);
    }

    public void resolve(String notes) throws InvalidStatusTransitionException {
        changeStatus(ComplaintStatus.RESOLVED, notes);
        this.resolutionNotes = notes;
    }

    private void addHistory(String note) {
        history.add(dateFiled.format(FMT) + " | " + note);
    }

    public long minutesSinceUpdate() {
        return java.time.Duration.between(lastUpdated, LocalDateTime.now()).toMinutes();
    }

    public boolean isOpenForEscalation() {
        return status == ComplaintStatus.OPEN || status == ComplaintStatus.IN_PROGRESS;
    }

    // --- getters ---
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public Priority getPriority() { return priority; }
    public ComplaintStatus getStatus() { return status; }
    public Student getFiledBy() { return filedBy; }
    public LocalDateTime getDateFiled() { return dateFiled; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public String getResolutionNotes() { return resolutionNotes; }
    public int getEscalationLevel() { return escalationLevel; }
    public List<String> getHistory() { return history; }
    public String getDateFiledFormatted() { return dateFiled.format(FMT); }

    @Override
    public int compareTo(Complaint other) {
        int p = other.priority.ordinal() - this.priority.ordinal(); // higher priority first
        if (p != 0) return p;
        return this.dateFiled.compareTo(other.dateFiled); // older first
    }
}
