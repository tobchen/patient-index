package de.tobchen.health.patientindex.feed.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.FhirContext;

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
}
