package com.cms.enums;

/**
 * Priority of a complaint. Order matters: ordinal 0 = lowest urgency.
 * Each level carries an SLA (in minutes) used by the escalation engine
 * to decide when an unresolved complaint must be auto-escalated.
 */
public enum Priority {
    LOW(10),
    MEDIUM(5),
    HIGH(2),
    CRITICAL(1);

    private final int slaMinutes;

    Priority(int slaMinutes) {
        this.slaMinutes = slaMinutes;
    }

    public int getSlaMinutes() {
        return slaMinutes;
    }

    /** Returns the next more urgent priority, or CRITICAL if already at the top. */
    public Priority escalateUp() {
        int next = Math.min(this.ordinal() + 1, values().length - 1);
        return values()[next];
    }
}
