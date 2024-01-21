package de.tobchen.health.patientindex.feed.components;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import de.tobchen.health.patientindex.feed.model.entities.MergeMessageEntity;
import de.tobchen.health.patientindex.feed.model.entities.MessageEntity;
import de.tobchen.health.patientindex.feed.model.repositories.MessageRepository;

@Component
public class PatientPoller
{
    private final Logger logger = LoggerFactory.getLogger(PatientPoller.class);
    
    private final MessageRepository repository;

    private final IGenericClient client;

    private final Instant checkFallback = Instant.now();

    public PatientPoller(MessageRepository repository, IGenericClient client)
    {
        this.repository = repository;
        this.client = client;
    }

    @Scheduled(fixedDelay = 10000)
    public synchronized void poll()
    {
        var mostRecentOccurence = repository.findTopByOrderByOccuredAtDesc().orElse(null);
        var checkFrom = mostRecentOccurence != null ? mostRecentOccurence.getOccuredAt() : checkFallback;

        try
        {
            var bundle = client.search()
                .forResource(Patient.class)
                .where(new DateClientParam(Constants.PARAM_LASTUPDATED)
                    .after()
                    .millis(Date.from(checkFrom)))
                .returnBundle(Bundle.class)
                .execute();
            
            var patientEvents = new ArrayList<PatientEvent>();
            
            var entries = bundle.getEntry();
            if (entries != null)
            {
                for (var entry : entries)
                {
                    if (entry.getResource() instanceof Patient patient)
                    {
                        var meta = patient.getMeta();
                        if (meta != null)
                        {
                            var lastUpdated = meta.getLastUpdated();
                            if (lastUpdated != null)
                            {
                                patientEvents.add(new PatientEvent(lastUpdated.toInstant(), patient));
                            }
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
    }

    @Transactional
    private Iterable<Long> createMessages(Iterable<PatientEvent> patientEvents)
    {
        var entities = new ArrayList<MessageEntity>();

        for (var patientEvent : patientEvents)
        {
            var patient = patientEvent.patient();

            var patientId = patient.getId();
            if (patientId != null)
            {
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
                                    entities.add(new MergeMessageEntity(referenceId, patientId, patientEvent.occuredAt()));
                                }
                                else
                                {
                                    logger.warn("Missing reference id for patient {}", patientId);
                                }
                            }
                            else
                            {
                                logger.warn("Weird link referenced id for patient {}", patientId);
                            }
                        }
                        else
                        {
                            logger.warn("Got null link reference for patient {}", patientId);
                        }
                    }
                    else
                    {
                        logger.warn("Got null link for patient {}", patientId);
                    }
                }
                else
                {
                    entities.add(new MessageEntity(patientId, patientEvent.occuredAt()));
                }
            }
            else
            {
                logger.warn("Got null id for patient");
            }
        }

        var savedEntities = repository.saveAll(entities);

        var ids = new ArrayList<Long>();

        for (var entity : savedEntities)
        {
            ids.add(entity.getId());
        }

        return ids;
    }

    private record PatientEvent(Instant occuredAt, Patient patient) { }
}
