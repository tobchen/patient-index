package de.tobchen.health.patientindex.commons.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "patient-index")
public record PatientIndexConfig(
    @DefaultValue
    Fhir fhir,
    @DefaultValue
    AssigningAuthority pid,
    @DefaultValue
    Feed feed
) {
    public record Fhir(
        String server
    ) { }

    public record AssigningAuthority(
        String namespace,
        String oid
    ) { }

    public record Feed(
        @DefaultValue
        Sender sender,
        @DefaultValue
        Receiver receiver,
        @DefaultValue("T")
        String processingMode
    ) {
        public record Sender(
            @DefaultValue
            AssigningAuthority application,
            @DefaultValue
            AssigningAuthority facility
        ) { }

        public record Receiver(
            @DefaultValue
            AssigningAuthority application,
            @DefaultValue
            AssigningAuthority facility,
            String host,
            Integer port
        ) { }
    }
}
