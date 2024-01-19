package de.tobchen.health.patientindex.servlets;

import javax.servlet.ServletException;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import de.tobchen.health.patientindex.services.PatientProvider;

public class FhirServlet extends RestfulServer
{
    private final PatientProvider patientProvider;

    public FhirServlet(FhirContext context, PatientProvider patientProvider)
    {
        super(context);
        
        this.patientProvider = patientProvider;
    }

    @Override
    protected void initialize() throws ServletException
    {
        super.initialize();

        setResourceProviders(patientProvider);
    }
}
