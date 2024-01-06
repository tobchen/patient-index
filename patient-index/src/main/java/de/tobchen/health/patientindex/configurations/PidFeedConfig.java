package de.tobchen.health.patientindex.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "patient-index.ihe.patient-identify-feed")
public record PidFeedConfig(
    Receiver receiver
) {
    public record Receiver(
        String appOid,
        String facilityOid
    ) { }
}
