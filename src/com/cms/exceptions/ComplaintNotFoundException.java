package com.cms.exceptions;

public class ComplaintNotFoundException extends Exception {
    public ComplaintNotFoundException(String complaintId) {
        super("No complaint found with id: " + complaintId);
    }
}
