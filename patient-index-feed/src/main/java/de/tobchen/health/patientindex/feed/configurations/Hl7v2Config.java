package de.tobchen.health.patientindex.feed.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.parser.Parser;

@Configuration
public class Hl7v2Config
{
    @Bean
    public HapiContext hl7v2Context()
    {
        return new DefaultHapiContext();
    }

    @Bean
    public Parser hl7v2Parser(HapiContext hl7v2Context)
    {
        return hl7v2Context.getPipeParser();
    }
}
