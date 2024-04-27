package de.tobchen.health.patientindex.main.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Meta;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.Patient.LinkType;
import org.hl7.fhir.r5.model.Reference;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.impl.DSL;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import static de.tobchen.health.patientindex.main.jooq.public_.Tables.*;

import de.tobchen.health.patientindex.main.events.ResourceChangeEvent;
import de.tobchen.health.patientindex.main.jooq.public_.tables.records.PatientRecord;

@Service
public class PatientService
{
    private final ApplicationEventPublisher publisher;

    private final DSLContext dsl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PatientService(ApplicationEventPublisher publisher, DSLContext dsl)
    {
        this.publisher = publisher;

        this.dsl = dsl;
    }

    public MethodOutcome createOrUpdate(Patient patient)
        throws JsonProcessingException
    {
        var identifierList = new ArrayList<IdentifierRecord>();

        var patientIdentifiers = patient.getIdentifier();
        if (patientIdentifiers != null)
        {
            for (var patientIdentifier : patientIdentifiers)
            {
                if (patientIdentifier != null)
                {
                    var system = patientIdentifier.getSystem();
                    var value = patientIdentifier.getValue();
                    if (system != null && value != null)
                    {
                        identifierList.add(new IdentifierRecord(system, value));
                    }
                }
            }
        }

        var identifierJson = objectMapper.writeValueAsString(identifierList);

        var transactionResult = dsl.transactionResult(trx -> {
            var resourceIdPart = patient.getIdPart();

            boolean created;
            PatientRecord patientRecord;

            if (resourceIdPart == null)
            {
                do
                {
                    resourceIdPart = UUID.randomUUID().toString();
                }
                while (trx.dsl().selectOne().from(PATIENT)
                    .where(PATIENT.ID.equal(resourceIdPart)).fetchAny() != null);
                
                created = true;
            }
            else
            {
                var mergedIntoRecord = trx.dsl()
                    .select(PATIENT.MERGED_INTO)
                    .from(PATIENT)
                    .where(PATIENT.ID.equal(resourceIdPart))
                    .fetchAny();
                
                if (mergedIntoRecord == null)
                {
                    created = true;
                }
                else if (mergedIntoRecord.value1() == null)
                {
                    created = false;
                }
                else
                {
                    throw new UnprocessableEntityException("Cannot update merged resource");
                }
            }

            if (created)
            {
                patientRecord = trx.dsl().insertInto(PATIENT)
                    .set(PATIENT.ID, resourceIdPart)
                    .set(PATIENT.LAST_UPDATED, DSL.currentOffsetDateTime())
                    .set(PATIENT.IDENTIFIERS, JSONB.jsonb(identifierJson))
                    .returningResult(PATIENT)
                    .fetchAny().value1();
            }
            else
            {
                patientRecord = trx.dsl().update(PATIENT)
                    .set(PATIENT.LAST_UPDATED, DSL.currentOffsetDateTime())
                    .set(PATIENT.IDENTIFIERS, JSONB.jsonb(identifierJson))
                    .where(PATIENT.ID.equal(resourceIdPart))
                    .returningResult(PATIENT)
                    .fetchAny().value1();
            }

            return new CreateOrUpdateTransactionResult(created, patientRecord);
        });

        var resource = resourceFromRecord(transactionResult.record());

        publisher.publishEvent(new ResourceChangeEvent(resource));

        var outcome = new MethodOutcome(resource.getIdElement(), transactionResult.created());
        outcome.setResource(resource);

        return outcome;
    }

    public Patient get(IIdType id)
    {
        Patient resource;

        var record = dsl.select(PATIENT)
            .from(PATIENT)
            .where(PATIENT.ID.equal(id.getIdPart()))
            .fetchAny();
        if (record != null)
        {
            try {
                resource = resourceFromRecord(record.value1());
            } catch (JsonProcessingException e) {
                throw new InternalErrorException("Cannot generate resource", e);
            }
        }
        else
        {
            resource = null;
        }

        return resource;
    }

    public List<Patient> findByIdentifier(String system, String value)
    {
        var result = new ArrayList<Patient>();

        var identifier = new IdentifierRecord(system, value);
        String identifierJson;
        try {
            identifierJson = objectMapper.writeValueAsString(new IdentifierRecord[] { identifier });
        } catch (JsonProcessingException e) {
            throw new InternalErrorException("Cannot generate search parameter", e);
        }

        var records = dsl.select(PATIENT)
            .from(PATIENT)
            .where(PATIENT.IDENTIFIERS.contains(JSONB.jsonb(identifierJson)))
            .fetch();
        for (var record : records)
        {
            Patient resource;
            try {
                resource = resourceFromRecord(record.value1());
            } catch (JsonProcessingException e) {
                throw new InternalErrorException("Cannot generate resource", e);
            }
            result.add(resource);
        }

        return result;
    }

    public MergeResult merge(IIdType sourceId, IIdType targetId)
    {
        var sourceIdPart = sourceId.getIdPart();
        var targetIdPart = targetId.getIdPart();

        var transactionResult = dsl.transactionResult(trx -> {
            var targetFetch = trx.dsl().select(PATIENT)
                .from(PATIENT)
                .where(PATIENT.ID.equal(targetIdPart))
                .fetchAny();
            if (targetFetch == null)
            {
                throw new InvalidRequestException("Target does not exist");
            }
            
            var targetRecord = targetFetch.value1();
            if (targetRecord.getMergedInto() != null)
            {
                throw new UnprocessableEntityException("Target is already merged");
            }

            var sourceFetch = trx.dsl().select(PATIENT.MERGED_INTO)
                .from(PATIENT)
                .where(PATIENT.ID.equal(sourceIdPart))
                .fetchAny();
            if (sourceFetch == null)
            {
                throw new InvalidRequestException("Source does not exist");
            }

            if (sourceFetch.value1() != null)
            {
                throw new UnprocessableEntityException("Source is already merged");
            }

            var sourceRecord = trx.dsl().update(PATIENT)
                .set(PATIENT.LAST_UPDATED, DSL.currentOffsetDateTime())
                .set(PATIENT.MERGED_INTO, targetIdPart)
                .where(PATIENT.ID.equal(sourceIdPart))
                .returningResult(PATIENT)
                .fetchAny().value1();

            return new MergeTransactionResult(sourceRecord, targetRecord);
        });

        Patient sourcePatient;
        try {
            sourcePatient = resourceFromRecord(transactionResult.source());
        } catch (JsonProcessingException e) {
            throw new InternalErrorException("Cannot generate source resource", e);
        }

        publisher.publishEvent(new ResourceChangeEvent(sourcePatient));

        Patient targetPatient;
        try {
            targetPatient = resourceFromRecord(transactionResult.target());
        } catch (JsonProcessingException e) {
            throw new InternalErrorException("Cannot generate target resource", e);
        }

        return new MergeResult(sourcePatient, targetPatient);
    }

    private Patient resourceFromRecord(PatientRecord record) throws JsonProcessingException
    {
        var resource = new Patient();

        resource.setIdElement(new IdType("Patient", record.getId()));

        resource.setMeta(new Meta()
            .setLastUpdated(Date.from(record.getLastUpdated().toInstant()))
        );

        var identifiers = objectMapper.readValue(record.getIdentifiers().data(), IdentifierRecord[].class);
        for (var identifier : identifiers)
        {
            resource.addIdentifier().setSystem(identifier.system()).setValue(identifier.value());
        }

        var mergedInto = record.getMergedInto();
        if (mergedInto != null)
        {
            resource.setActive(false);

            resource.addLink()
                .setOther(new Reference(new IdType("Patient", mergedInto)))
                .setType(LinkType.REPLACEDBY);
        }
        else
        {
            resource.setActive(true);
        }

        return resource;
    }

    public record MergeResult(Patient source, Patient target) { }

    private record IdentifierRecord(String system, String value) { }

    private record CreateOrUpdateTransactionResult(boolean created, PatientRecord record) { }

    private record MergeTransactionResult(PatientRecord source, PatientRecord target) { }
}
