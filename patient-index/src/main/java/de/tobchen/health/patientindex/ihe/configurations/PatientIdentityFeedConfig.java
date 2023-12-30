package de.tobchen.health.patientindex.ihe.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "patient-index.ihe.patient-identify-feed")
public record PatientIdentityFeedConfig(
    Receiver receiver
) {
    public record Receiver(
        String appOid,
        String facilityOid
    ) { }
}
