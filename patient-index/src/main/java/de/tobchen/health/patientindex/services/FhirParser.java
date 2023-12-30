package de.tobchen.health.patientindex.services;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@Service
public class FhirParser
{
    private final IParser parser;

    public FhirParser(FhirContext context)
    {
        this.parser = context.newJsonParser();
    }

    synchronized public String resourceToString(IBaseResource resource)
    {
        return parser.encodeResourceToString(resource);
    }

    synchronized public <T extends IBaseResource> T stringToResource(String string, Class<T> resourceType)
    {
        return parser.parseResource(resourceType, string);
    }
}
