package com.cms.exceptions;

import com.cms.enums.ComplaintStatus;

public class InvalidStatusTransitionException extends Exception {
    public InvalidStatusTransitionException(ComplaintStatus from, ComplaintStatus to) {
        super("Cannot move complaint from " + from + " to " + to);
    }
}
