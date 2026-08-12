package com.cms.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle states of a complaint, with a built-in table of legal
 * transitions so the model can reject illegal status changes itself
 * (encapsulation of business rules inside the enum).
 */
public enum ComplaintStatus {
    OPEN,
    IN_PROGRESS,
    ESCALATED,
    RESOLVED,
    CLOSED,
    REOPENED;

    public boolean canTransitionTo(ComplaintStatus target) {
        return allowedNext().contains(target);
    }

    private Set<ComplaintStatus> allowedNext() {
        switch (this) {
            case OPEN:
                return EnumSet.of(IN_PROGRESS, ESCALATED);
            case IN_PROGRESS:
                return EnumSet.of(RESOLVED, ESCALATED);
            case ESCALATED:
                return EnumSet.of(IN_PROGRESS, RESOLVED);
            case RESOLVED:
                return EnumSet.of(CLOSED, REOPENED);
            case REOPENED:
                return EnumSet.of(IN_PROGRESS, ESCALATED);
            case CLOSED:
                return EnumSet.noneOf(ComplaintStatus.class);
            default:
                return EnumSet.noneOf(ComplaintStatus.class);
        }
    }
}
