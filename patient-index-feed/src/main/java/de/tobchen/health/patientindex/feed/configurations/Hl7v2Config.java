package de.tobchen.health.patientindex.feed.configurations;

import org.springframework.context.annotation.Configuration;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;

@Configuration
public class Hl7v2Config
{
    public HapiContext hl7v2Context()
    {
        return new DefaultHapiContext();
    }
}
