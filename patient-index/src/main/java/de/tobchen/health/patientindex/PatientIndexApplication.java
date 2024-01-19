package de.tobchen.health.patientindex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class PatientIndexApplication
{
	public static void main(String[] args)
	{
		SpringApplication.run(PatientIndexApplication.class, args);
	}
}
