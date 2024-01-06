package de.tobchen.health.patientindex.model.dto;

public record PatientMergeDto(
    String sourceResourceId,
    String targetResourceId
) { }
