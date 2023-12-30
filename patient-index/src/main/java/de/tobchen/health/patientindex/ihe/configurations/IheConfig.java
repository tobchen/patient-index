package de.tobchen.health.patientindex.ihe.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "patient-index.ihe")
public record IheConfig(
    String pidOid,
    String appOid,
    String facilityOid
) { }
