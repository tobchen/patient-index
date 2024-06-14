package de.tobchen.health.patientindex.ws.configurations;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.tobchen.health.patientindex.commons.configurations.PatientIndexConfig;

@Configuration
@EnableConfigurationProperties(PatientIndexConfig.class)
public class FhirConfig
{
    @Bean
    public FhirContext fhirContext()
    {
        var context = FhirContext.forR5();
        if (context == null)
        {
            throw new RuntimeException("Cannot get FHIR R5 context");
        }

        return context;
    }

    @Bean
    public IGenericClient fhirClient(
        FhirContext context, PatientIndexConfig config)
    {
        var client = context.newRestfulGenericClient(config.fhir().server());
        if (client == null)
        {
            throw new RuntimeException("Cannot init client");
        }

        return client;
    }
}
