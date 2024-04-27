package de.tobchen.health.patientindex.main.events;

import org.hl7.fhir.r5.model.Resource;

public record ResourceChangeEvent(Resource resource) { }
