package de.tobchen.health.patientindex.feed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PatientIndexFeedApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(PatientIndexFeedApplication.class, args);
    }
}
