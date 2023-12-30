package de.tobchen.health.patientindex.ihe.model.records;

import de.tobchen.health.patientindex.ihe.model.enums.MessageStatus;

public record PatientIdentityFeedMessageRecord(
    Long id,
    MessageStatus status,
    String message,
    String response
) { }
