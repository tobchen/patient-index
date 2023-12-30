package de.tobchen.health.patientindex.ihe.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;

@Configuration
public class HapiContextConfig
{
    @Bean
    public HapiContext hapiContext()
    {
        return new DefaultHapiContext();
    }
}
