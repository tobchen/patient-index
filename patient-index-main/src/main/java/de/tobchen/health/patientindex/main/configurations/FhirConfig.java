package de.tobchen.health.patientindex.main.configurations;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.FhirContext;
import de.tobchen.health.patientindex.main.providers.PatientProvider;
import de.tobchen.health.patientindex.main.servlets.FhirServlet;

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
    public ServletRegistrationBean<FhirServlet> fhirServlet(FhirContext context, PatientProvider patientProvider)
    {
        return new ServletRegistrationBean<>(new FhirServlet(context, patientProvider), "/fhir/r5/*");
    }
}
