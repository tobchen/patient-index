package de.tobchen.health.patientindex.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "patient-index")
public record PatientIndexConfig(Fhir fhir, AssigningAuthority pid, Feed feed)
{
    public record Fhir(String server) { }

    public record AssigningAuthority(String namespace, String oid) { }

    public record Feed(Sender sender, Receiver receiver, String processingMode)
    {
        public record Sender(AssigningAuthority application, AssigningAuthority facility) { }

        public record Receiver(AssigningAuthority application, AssigningAuthority facility,
            String host, Integer port) { }
    }
}
