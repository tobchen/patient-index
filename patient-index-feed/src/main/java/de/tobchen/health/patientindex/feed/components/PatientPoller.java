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

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import de.tobchen.health.patientindex.feed.model.repositories.MessageRepository;

@Component
public class PatientPoller
{
    private final Logger logger = LoggerFactory.getLogger(PatientPoller.class);
    
    private final MessageRepository repository;

    private final IGenericClient client;

    private Instant lastCheckedAt = Instant.now();

    public PatientPoller(MessageRepository repository, IGenericClient client)
    {
        this.repository = repository;
        this.client = client;
    }

    @Scheduled(fixedDelay = 10000)
    public synchronized void poll()
    {
        updateLastCheckedAt();

        for (var patientEvent : getSortedPatientEvents(lastCheckedAt))
        {
            lastCheckedAt = patientEvent.occuredAt();
        }
    }

    private void updateLastCheckedAt()
    {
        var mostRecentOccurence = repository.findTopByOrderByOccuredAtDesc().orElse(null);
        if (mostRecentOccurence != null)
        {
            lastCheckedAt = mostRecentOccurence.getOccuredAt();
        }
    }

    private Iterable<PatientEvent> getSortedPatientEvents(Instant from)
    {
        var result = new ArrayList<PatientEvent>();

        try
        {
            var bundle = client.search()
                .forResource(Patient.class)
                .where(new DateClientParam(Constants.PARAM_LASTUPDATED).after().millis(new Date()))
                .returnBundle(Bundle.class)
                .execute();
            
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
                                result.add(new PatientEvent(lastUpdated.toInstant(), patient));
                            }
                        }    
                    }
                }
            }
        }
        catch (FhirClientConnectionException e)
        {
            logger.warn("Cannot connect to FHIR server");
        }

        result.sort((a, b) -> { return a.occuredAt().compareTo(b.occuredAt()); });

        return result;
    }

    private record PatientEvent(Instant occuredAt, Patient patient) { }
}
