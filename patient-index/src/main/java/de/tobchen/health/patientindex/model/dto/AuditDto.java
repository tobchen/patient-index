package de.tobchen.health.patientindex.model.dto;

import java.util.Date;

import org.hl7.fhir.r5.model.AuditEvent.AuditEventAction;
import org.springframework.lang.Nullable;

public record AuditDto(
    String categorySystem,
    String categoryCode,
    String codeSystem,
    String codeCode,
    AuditEventAction action,
    Date recordedAt,
    String patientResourceId,
    String sourceDisplay,
    @Nullable
    String sourceAddress,
    String destinationDisplay,
    @Nullable
    String destinationAddress,
    String query
) { }
