package de.tobchen.health.patientindex.feed.transformers;

import java.nio.charset.StandardCharsets;

import org.hl7.fhir.r5.model.Patient;
import org.springframework.integration.transformer.AbstractPayloadTransformer;

import ca.uhn.fhir.context.FhirContext;

public class BytesToPatientTransformer extends AbstractPayloadTransformer<byte[], Patient>
{
    private final FhirContext context;

    public BytesToPatientTransformer(FhirContext context)
    {
        this.context = context;
    }

    @Override
    protected Patient transformPayload(byte[] payload)
    {
        return context.newJsonParser().parseResource(Patient.class,
            new String(payload, StandardCharsets.UTF_8));
    }    
}
