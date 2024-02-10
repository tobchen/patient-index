package de.tobchen.health.patientindex.feed.components;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
            var setup = getPollingSetup();

            var patientResults = getPatientResults(setup);

            var messageIds = createMessages(patientResults);

            for (var messageId : messageIds)
            {
                if (messageId != null)
                {
                    sender.queue(messageId);
                }
            }
        }
        finally
        {
            span.end();
        }
    }

    private List<PatientResult> getPatientResults(PollingSetup setup)
    {
        var patientResults = new ArrayList<PatientResult>();

        try
        {
            var query = client.search()
                .forResource(Patient.class)
                .where(new DateClientParam(Constants.PARAM_LASTUPDATED)
                    .after()
                    .millis(Date.from(setup.searchFrom())))
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

                        var id = patient.getIdPart();
                        if (id != null)
                        {
                            var meta = patient.getMeta();
                            if (meta != null)
                            {
                                var versionId = meta.getVersionId();
                                if (versionId != null)
                                {
                                    var idVid = new PatientIdVid(id, versionId);
                                    if (setup.whitelist() == null || !setup.whitelist().contains(idVid))
                                    {
                                        var lastUpdated = meta.getLastUpdated();
                                        if (lastUpdated != null)
                                        {
                                            try
                                            {
                                                var linkedId = getLinkedPatientId(patient);

                                                patientResults.add(new PatientResult(idVid,
                                                    lastUpdated.toInstant(), linkedId));
                                            }
                                            catch (IllegalArgumentException e)
                                            {
                                                logger.info("Ignoring patient {} with bad link", id);
                                            }
                                        }
                                        else
                                        {
                                            logger.info("Ignoring patient {} without lastUpdated", id);
                                        }
                                    }
                                    else
                                    {
                                        logger.info("Ignoring whitelisted patient {}", id);
                                    }
                                }
                                else
                                {
                                    logger.info("Ignoring patient {} without version", id);
                                }
                            }
                            else
                            {
                                logger.info("Ignoring patient {} without meta", id);
                            }
                        }
                        else
                        {
                            logger.info("Ignoring patient without id");
                        }
                    }
                }
            }

            patientResults.sort((a, b) -> { return a.updatedAt().compareTo(b.updatedAt()); });
        }
        catch (FhirClientConnectionException e)
        {
            logger.warn("Cannot connect to FHIR server");
        }

        return patientResults;
    }

    @Transactional
    private List<Long> createMessages(List<PatientResult> patientResults)
    {
        var entities = new ArrayList<MessageEntity>();

        for (var result : patientResults)
        {
            entities.add(new MessageEntity(result.idVid().id(), result.idVid().versionId(),
                result.updatedAt(), result.linkedPatientId()));
        }

        var savedEntities = repository.saveAll(entities);

        var ids = new ArrayList<Long>();
        for (var entity : savedEntities)
        {
            ids.add(entity.getId());
        }

        return ids;
    }

    private static @Nullable String getLinkedPatientId(Patient patient) throws IllegalArgumentException
    {
        String linkedPatientId;

        var linkList = patient.getLink();
        if (linkList != null && linkList.size() > 0)
        {
            var link = linkList.get(0);
            if (link != null)
            {
                var reference = link.getOther();
                if (reference != null)
                {
                    var referenceIdType = reference.getReferenceElement();
                    if (!referenceIdType.hasBaseUrl() && "Patient".equals(referenceIdType.getResourceType()))
                    {
                        var referenceId = referenceIdType.getIdPart();
                        if (referenceId != null)
                        {
                            linkedPatientId = referenceId;
                        }
                        else
                        {
                            throw new IllegalArgumentException("Missing reference id value");
                        }
                    }
                    else
                    {
                        throw new IllegalArgumentException("Bad reference id type");
                    }
                }
                else
                {
                    throw new IllegalArgumentException("Null reference found");
                }
            }
            else
            {
                throw new IllegalArgumentException("Null link found");
            }
        }
        else
        {
            linkedPatientId = null;
        }

        return linkedPatientId;
    }

    @Transactional(readOnly = true)
    private PollingSetup getPollingSetup()
    {
        PollingSetup result;

        var topUpdatedAt = repository.findTopByOrderByPatientUpdatedAtDesc().orElse(null);
        if (topUpdatedAt != null)
        {
            var searchFrom = topUpdatedAt.getPatientUpdatedAt().minusSeconds(1);

            var whitelistStart = searchFrom.minusSeconds(1);
            var whitelist = new HashSet<PatientIdVid>();
            for (var patientIdAndVersionId : repository.findByPatientUpdatedAtGreaterThanEqual(whitelistStart))
            {
                whitelist.add(new PatientIdVid(patientIdAndVersionId.getPatientId(),
                    patientIdAndVersionId.getPatientVersionId()));
            }

            result = new PollingSetup(searchFrom, whitelist);
        }
        else
        {
            result = new PollingSetup(checkFallback, null);
        }

        return result;
    }

    private record PollingSetup(Instant searchFrom, @Nullable Set<PatientIdVid> whitelist) { };

    private record PatientIdVid(String id, @Nullable String versionId) { };

    private record PatientResult(PatientIdVid idVid, Instant updatedAt, @Nullable String linkedPatientId) { }
}
