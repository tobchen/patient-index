package de.tobchen.health.patientindex.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class PatientIndexMainApplication
{
	public static void main(String[] args)
	{
		SpringApplication.run(PatientIndexMainApplication.class, args);
	}
}
