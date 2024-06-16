package de.tobchen.health.patientindex.feed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import de.tobchen.health.patientindex.commons.configurations.PatientIndexConfig;

@SpringBootApplication
@EnableConfigurationProperties(PatientIndexConfig.class)
public class PatientIndexFeedApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(PatientIndexFeedApplication.class, args);
    }
}
