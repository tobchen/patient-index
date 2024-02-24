package de.tobchen.health.patientindex.feed.components;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import ca.uhn.fhir.rest.gclient.IQuery;
import de.tobchen.health.patientindex.feed.model.entities.MessageEntity;
import de.tobchen.health.patientindex.feed.model.repositories.MessageRepository;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;

@Component
public class PatientPoller
{
    private final Logger logger = LoggerFactory.getLogger(PatientPoller.class);

    private final Tracer tracer;
    private final TextMapPropagator propagator;
    private final TextMapSetter<IQuery<Bundle>> otelSetter;
    
    private final MessageRepository repository;

    private final IGenericClient client;

    private final MessageSender sender;

    private final Instant checkFallback = Instant.now();

    public PatientPoller(OpenTelemetry openTelemetry, MessageRepository repository,
        IGenericClient client, MessageSender sender)
    {
        this.tracer = openTelemetry.getTracer(PatientPoller.class.getName());
        this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
        this.otelSetter = new TextMapSetter<IQuery<Bundle>>()
        {
            @Override
            public void set(@Nullable IQuery<Bundle> carrier,
                @Nullable String key, @Nullable String value)
            {
                if (carrier != null)
                {
                    carrier.withAdditionalHeader(key, value);
                }
            }
        };

        this.repository = repository;
        this.client = client;
        this.sender = sender;
    }

    @Scheduled(fixedDelay = 15, timeUnit = TimeUnit.SECONDS)
    public synchronized void poll()
    {
        var span = tracer.spanBuilder("PatientPoller.poll").startSpan();

        try (var scope = span.makeCurrent())
        {
            Instant searchFrom = checkFallback;
            var whitelist = new HashSet<PatientIdVid>();

            var topUpdatedAt = repository.findTopByOrderByPatientUpdatedAtDesc().orElse(null);
            if (topUpdatedAt != null)
            {
                searchFrom = topUpdatedAt.getPatientUpdatedAt().minusSeconds(1);

                var whitelistStart = searchFrom.minusSeconds(1);
                
                for (var patientIdAndVersionId : repository.findByPatientUpdatedAtGreaterThanEqual(whitelistStart))
                {
                    whitelist.add(new PatientIdVid(patientIdAndVersionId.getPatientId(),
                        patientIdAndVersionId.getPatientVersionId()));
                }
            }

            var newMessages = new ArrayList<MessageEntity>();

            try
            {
                var query = client.search()
                    .forResource(Patient.class)
                    .where(new DateClientParam(Constants.PARAM_LASTUPDATED)
                        .after()
                        .millis(Date.from(searchFrom)))
                    .returnBundle(Bundle.class);
                
                propagator.inject(Context.current(), query, otelSetter);
                
                var bundle = query.execute();
                
                var entries = bundle.getEntry();
                if (entries != null)
                {
                    for (var entry : entries)
                    {
                        var resource = entry.getResource();
                        if (!(resource instanceof Patient))
                        {
                            logger.info("Ignoring non Patient resource");
                            continue;
                        }

                        var patient = (Patient) resource;
                        var id = patient.getIdPart();
                        if (id == null)
                        {
                            logger.info("Ignoring patient without id");
                            continue;
                        }

                        var meta = patient.getMeta();
                        if (meta == null)
                        {
                            logger.info("Ignoring patient {} without meta", id);
                            continue;
                        }

                        var versionId = meta.getVersionId();
                        if (versionId == null)
                        {
                            logger.info("Ignoring patient {} without version", id);
                            continue;
                        }

                        var idVid = new PatientIdVid(id, versionId);
                        if (whitelist.contains(idVid))
                        {
                            logger.info("Ignoring whitelisted patient {}", id);
                            continue;
                        }

                        var lastUpdated = meta.getLastUpdated();
                        if (lastUpdated == null)
                        {
                            logger.info("Ignoring patient {} without lastUpdated", id);
                            continue;
                        }

                        String linkedId = null;

                        var linkList = patient.getLink();
                        if (linkList != null && linkList.size() > 0)
                        {
                            for (var link : linkList)
                            {
                                var reference = link.getOther();
                                if (reference == null)
                                {
                                    logger.info("Ignoring null link reference for patient {}", id);
                                    continue;
                                }

                                var referenceIdType = reference.getReferenceElement();
                                if (!"Patient".equals(referenceIdType.getResourceType()))
                                {
                                    logger.info("Ignoring non patient link for patient {}", id);
                                    continue;
                                }

                                linkedId = referenceIdType.getIdPart();
                                if (linkedId == null)
                                {
                                    logger.info("Ignoring non patient id link for patient {}", id);
                                }

                                break;
                            }

                            if (linkedId == null)
                            {
                                logger.info("Ignoring patient {} with bad link", id);
                                continue;
                            }
                        }

                        newMessages.add(new MessageEntity(
                            idVid.id(), idVid.versionId(), lastUpdated.toInstant(), linkedId));
                    }
                }
            }
            catch (FhirClientConnectionException e)
            {
                logger.warn("Cannot connect to FHIR server");
            }

            newMessages.sort((a, b) -> a.getPatientUpdatedAt().compareTo(b.getPatientUpdatedAt()));

            for (var entity : repository.saveAll(newMessages))
            {
                sender.queue(entity.getId());
            }
        }
        finally
        {
            span.end();
        }
    }

    private record PatientIdVid(String id, @Nullable String versionId) { };
}
