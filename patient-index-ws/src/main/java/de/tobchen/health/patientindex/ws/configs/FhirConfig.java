package de.tobchen.health.patientindex.ws.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

@Configuration
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
        FhirContext context, @Value("${patient-index.fhir.server}") String base)
    {
        var client = context.newRestfulGenericClient(base);
        if (client == null)
        {
            throw new RuntimeException("Cannot init client");
        }

        return client;
    }
}
