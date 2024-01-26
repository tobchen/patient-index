package de.tobchen.health.patientindex.feed.components;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import de.tobchen.health.patientindex.feed.model.entities.MessageEntity;
import de.tobchen.health.patientindex.feed.model.repositories.MessageRepository;

@Component
public class PatientPoller
{
    private final Logger logger = LoggerFactory.getLogger(PatientPoller.class);
    
    private final MessageRepository repository;

    private final MessageSender sender;

    private final IGenericClient client;

    private final Instant checkFallback = Instant.now();

    public PatientPoller(MessageRepository repository, MessageSender sender, IGenericClient client)
    {
        this.repository = repository;
        this.sender = sender;
        this.client = client;
    }

    @Scheduled(fixedDelay = 10000)
    public synchronized void poll()
    {
        var mostRecentPatientUpdatedAt = getMostRecentMessagePatientUpdatedAt();
        var checkFrom = mostRecentPatientUpdatedAt != null ? mostRecentPatientUpdatedAt : checkFallback;

        var patientEvents = getPatientEvents(checkFrom);

        for (var messageId : createMessages(patientEvents))
        {
            if (messageId != null)
            {
                sender.queue(messageId);
            }
        }
    }

    private List<PatientEvent> getPatientEvents(Instant checkFrom)
    {
        var patientEvents = new ArrayList<PatientEvent>();

        try
        {
            var bundle = client.search()
                .forResource(Patient.class)
                .where(new DateClientParam(Constants.PARAM_LASTUPDATED)
                    .after()
                    .millis(Date.from(checkFrom)))
                .returnBundle(Bundle.class)
                .execute();
            
            var entries = bundle.getEntry();
            if (entries != null)
            {
                for (var entry : entries)
                {
                    var resource = entry.getResource();
                    if (resource instanceof Patient)
                    {
                        var patient = (Patient) resource;

                        var patientId = patient.getIdPart();
                        if (patientId != null)
                        {
                            var meta = patient.getMeta();
                            if (meta != null)
                            {
                                var lastUpdated = meta.getLastUpdated();
                                if (lastUpdated != null)
                                {
                                    var versionId = meta.getVersionId();
                                    
                                    try
                                    {
                                        var otherPatientId = getOtherPatientId(patient);

                                        patientEvents.add(new PatientEvent(
                                            lastUpdated.toInstant(), patientId, versionId, otherPatientId));
                                    }
                                    catch (IllegalArgumentException e)
                                    {
                                        logger.warn("Failed to get other patient id for {}: {}",
                                            patientId, e.getMessage());
                                    }
                                }
                                else
                                {
                                    logger.warn("Received patient without lastUpdated, id: {}", patientId);
                                }
                            }  
                        }
                        else
                        {
                            logger.warn("Received patient without id");
                        }  
                    }
                }
            }

            patientEvents.sort((a, b) -> { return a.occuredAt().compareTo(b.occuredAt()); });
        }
        catch (FhirClientConnectionException e)
        {
            logger.warn("Cannot connect to FHIR server");
        }

        return patientEvents;
    }

    @Transactional
    private List<Long> createMessages(List<PatientEvent> patientEvents)
    {
        var entities = new ArrayList<MessageEntity>();

        for (var event : patientEvents)
        {
            entities.add(new MessageEntity(event.patientId(), event.versionId(),
                event.occuredAt(), event.otherPatientId()));
        }

        var savedEntities = repository.saveAll(entities);

        var ids = new ArrayList<Long>();

        for (var entity : savedEntities)
        {
            ids.add(entity.getId());
        }

        return ids;
    }

    private static @Nullable String getOtherPatientId(Patient patient) throws IllegalArgumentException
    {
        String otherPatientId;

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
                            otherPatientId = referenceId;
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
            otherPatientId = null;
        }

        return otherPatientId;
    }

    @Transactional(readOnly = true)
    private @Nullable Instant getMostRecentMessagePatientUpdatedAt()
    {
        var mostRecentMessage = repository.findTopByOrderByPatientUpdatedAtDesc().orElse(null);
        return mostRecentMessage != null ? mostRecentMessage.getPatientUpdatedAt() : null;
    }

    private record PatientEvent(Instant occuredAt, String patientId,
        @Nullable String versionId, @Nullable String otherPatientId) { }
}
