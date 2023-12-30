package de.tobchen.health.patientindex.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.FhirContext;

@Configuration
public class FhirConfig
{
    @Bean
    public FhirContext fhirContext()
    {
        return FhirContext.forR5();
    }
}
