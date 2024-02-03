package de.tobchen.health.patientindex.ws.services;

import java.util.ArrayList;
import java.util.Collection;

import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Patient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IClientExecutable;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import de.tobchen.health.patientindex.ws.model.schemas.II;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;

@Service
public class QueryService
{
    private final Tracer tracer;
    private final TextMapPropagator propagator;
    private final TextMapSetter<IClientExecutable<?, ?>> otelSetter;

    private final IGenericClient client;

    private final String pidOid;

    public QueryService(OpenTelemetry openTelemetry, IGenericClient client,
        @Value("${patient-index-ws.hl7v3.pid.oid}") String pidOid)
    {
        this.tracer = openTelemetry.getTracer(QueryService.class.getName());
        this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
        this.otelSetter =
            new TextMapSetter<IClientExecutable<?, ?>>()
        {
            @Override
            public void set(IClientExecutable<?, ?> carrier,
                String key, String value)
            {
                carrier.withAdditionalHeader(key, value);
            }  
        };
        
        this.client = client;

        this.pidOid = pidOid;
    }

    public Collection<II> findOtherIdentifiers(II queriedIi)
    {
        var span = tracer.spanBuilder("QueryService.findOtherIdentifiers").startSpan();

        try (var scope = span.makeCurrent())
        {
            var result = new ArrayList<II>();

            String queriedRoot = queriedIi.getRoot();
            String queriedExtension = queriedIi.getExtension();
            if (queriedRoot != null && queriedExtension != null)
            {
                if (queriedRoot.equals(pidOid))
                {
                    var executable = client.read().resource(Patient.class).withId(queriedExtension);

                    propagator.inject(Context.current(), executable, otelSetter);
                    
                    var patient = executable.execute();

                    addIdentifiers(result, patient, queriedRoot, queriedExtension);
                }
                else
                {
                    var query = client.search()
                        .forResource(Patient.class)
                        .where(new TokenClientParam(Patient.SP_IDENTIFIER)
                            .exactly()
                            .systemAndIdentifier("urn:oid:" + queriedRoot, queriedExtension))
                        .returnBundle(Bundle.class);
                    
                    propagator.inject(Context.current(), query, otelSetter);

                    var bundle = query.execute();

                    var entries = bundle.getEntry();
                    if (entries != null)
                    {
                        for (var entry : entries)
                        {
                            var resource = entry.getResource();
                            if (resource instanceof Patient)
                            {
                                var patient = (Patient) resource;

                                addIdentifiers(result, patient, queriedRoot, queriedExtension);
                            }
                        }
                    }
                }
            }

            return result;
        }
        finally
        {
            span.end();
        }
    }

    private void addIdentifiers(Collection<II> identifiers, Patient patient,
        String exceptSystem, String exceptValue)
    {
        var id = patient.getIdPart();
        if (!exceptSystem.equals(pidOid) || exceptValue.equals(id))
        {
            identifiers.add(createIi(pidOid, id));
        }

        var patientIdentifiers = patient.getIdentifier();
        if (patientIdentifiers != null)
        {
            for (var identifier : patientIdentifiers)
            {
                var system = identifier.getSystem();
                var value = identifier.getValue();
                if (system != null && system.startsWith("urn:oid:") && value != null)
                {
                    var systemOid = system.substring(8);
                    if (!exceptSystem.equals(systemOid) || !exceptValue.equals(value))
                    {
                        identifiers.add(createIi(systemOid, value));
                    }
                }
            }
        }
    }

    private static II createIi(String root, String extension)
    {
        var ii = new II();
        ii.setRoot(root);
        ii.setExtension(extension);
        return ii;
    }
}
