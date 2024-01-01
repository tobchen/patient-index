package de.tobchen.health.patientindex.model.dto;

import java.util.Date;

import org.hl7.fhir.r5.model.AuditEvent.AuditEventAction;

public record AuditDto(
    String categorySystem,
    String categoryCode,
    String codeSystem,
    String codeCode,
    AuditEventAction action,
    Date recordedAt,
    String patientResourceId,
    String sourceDisplay,
    String sourceAddress,
    String destinationDisplay,
    String destinationAddress,
    String query
) { }
