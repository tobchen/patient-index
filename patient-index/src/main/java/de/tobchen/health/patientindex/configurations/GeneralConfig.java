package de.tobchen.health.patientindex.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "patient-index")
public record GeneralConfig(
    Boolean productionMode
) { }
