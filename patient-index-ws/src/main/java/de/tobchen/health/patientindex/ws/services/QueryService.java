package de.tobchen.health.patientindex.ws.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Patient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IClientExecutable;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
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
        @Value("${patient-index.pid-oid}") String pidOid)
    {
        this.tracer = openTelemetry.getTracer(QueryService.class.getName());
        this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
        this.otelSetter = new TextMapSetter<IClientExecutable<?, ?>>() {
            @Override
            public void set(@Nullable IClientExecutable<?, ?> carrier,
                @Nonnull String key, @Nonnull String value)
            {
                if (carrier != null)
                {
                    carrier.withAdditionalHeader(key, value);
                }
            }  
        };
        
        this.client = client;

        this.pidOid = pidOid;
    }

    public Map<String, Set<String>> findIdentifiers(String system, String value)
    {
        var span = tracer.spanBuilder("QueryService.findOtherIdentifiers").startSpan();
        try (var scope = span.makeCurrent())
        {
            var systemValuesMap = new HashMap<String, Set<String>>();

            if (system.equals(pidOid))
            {
                var executable = client
                    .read()
                    .resource(Patient.class)
                    .withId(value);

                propagator.inject(Context.current(), executable, otelSetter);

                var patient = executable.execute();

                if (patient != null)
                {
                    populate(systemValuesMap, patient);
                }
            }
            else
            {
                var query = client
                    .search()
                    .forResource(Patient.class)
                    .where(new TokenClientParam(Patient.SP_IDENTIFIER)
                        .exactly()
                        .systemAndIdentifier(system, value)
                    )
                    .returnBundle(Bundle.class);
                
                propagator.inject(Context.current(), query, otelSetter);

                var bundle = query.execute();

                for (var entry : bundle.getEntry())
                {
                    var resource = entry.getResource();
                    if (resource instanceof Patient)
                    {
                        populate(systemValuesMap, (Patient) resource);
                    }
                }
            }

            return systemValuesMap;
        }
        finally
        {
            span.end();
        }
    }

    private void populate(Map<String, Set<String>> systemValuesMap, Patient patient)
    {
        // TODO Do not populate if patient is inactive

        populate(systemValuesMap, pidOid, patient.getIdPart());

        for (var identifier : patient.getIdentifier())
        {
            var system = identifier.getSystem();
            var value = identifier.getValue();
            if (system != null && system.startsWith("urn:oid:") && value != null)
            {
                populate(systemValuesMap, system.substring(8), value);
            }
        }
    }

    private void populate(Map<String, Set<String>> systemValuesMap, String system, String value)
    {
        var set = systemValuesMap.get(system);
        if (set == null)
        {
            set = new HashSet<>();
            systemValuesMap.put(system, set);
        }

        set.add(value);
    }
}
