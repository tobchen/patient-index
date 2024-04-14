package de.tobchen.health.patientindex.main.providers;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Meta;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r5.model.Patient.LinkType;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortOrderEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import static de.tobchen.health.patientindex.main.jooq.public_.Tables.*;
import de.tobchen.health.patientindex.main.jooq.public_.tables.records.PatientRecord;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Tracer;

@Service
public class PatientProvider implements IResourceProvider
{
    private final Logger logger = LoggerFactory.getLogger(PatientProvider.class);

    private final Tracer tracer;

    private final DSLContext dsl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PatientProvider(OpenTelemetry openTelemetry, DSLContext dsl)
    {
        this.tracer = openTelemetry.getTracer(PatientProvider.class.getName());
        this.dsl = dsl;
    }

    @Override
    public Class<? extends IBaseResource> getResourceType()
    {
        return Patient.class;
    }

    @Create
    public MethodOutcome create(@ResourceParam Patient patient) throws JsonProcessingException
    {
        var span = tracer.spanBuilder("PatientProvider.create").startSpan();

        try (var scope = span.makeCurrent())
        {
            var outcome = insertOrUpdatePatient(null, patient);

            span.setAttribute("audit.action", "create");
            span.setAttribute("audit.patient", outcome.getId().getIdPart());

            logger.debug("Patient created!");

            return outcome;
        }
        catch (Throwable t)
        {
            span.recordException(t);
            throw t;
        }
        finally
        {
            span.end();
        }
    }

    @Update
    public MethodOutcome update(@Nullable @IdParam IIdType idType, @ResourceParam Patient patient)
        throws JsonProcessingException
    {
        var span = tracer.spanBuilder("PatientProvider.update").startSpan();

        try (var scope = span.makeCurrent())
        {
            var idPart = idType.getIdPart();
            if (idPart == null)
            {
                throw new InvalidRequestException("Id is missing id part");
            }

            var outcome = insertOrUpdatePatient(idPart, patient);

            span.setAttribute("audit.action", outcome.getCreated().booleanValue() ? "create" : "update");
            span.setAttribute("audit.patient", outcome.getId().getIdPart());

            logger.debug("Patient created or updated!");

            return outcome;
        }
        catch (Throwable t)
        {
            span.recordException(t);
            throw t;
        }
        finally
        {
            span.end();
        }
    }

    @Read
    public @Nullable Patient read(@IdParam IIdType resourceId) throws JsonProcessingException
    {
        var span = tracer.spanBuilder("PatientProvider.read").startSpan();

        try (var scope = span.makeCurrent())
        {
            Patient resource;

            var record = dsl.select(PATIENT)
                .from(PATIENT)
                .where(PATIENT.ID.equal(resourceId.getIdPart()))
                .fetchAny();
            if (record != null)
            {
                resource = resourceFromRecord(record.value1());

                span.setAttribute("audit.action", "read");
                span.setAttribute("audit.patient", resource.getIdPart());
            }
            else
            {
                resource = null;
            }

            return resource;
        }
        catch (Throwable t)
        {
            span.recordException(t);
            throw t;
        }
        finally
        {
            span.end();
        }
    }

    @Search
    public List<Patient> searchByIdentifier(
        @RequiredParam(name = Patient.SP_IDENTIFIER) TokenParam resourceIdentifier)
        throws JsonProcessingException
    {
        var span = tracer.spanBuilder("PatientProvider.searchByIdentifier").startSpan();

        try (var scope = span.makeCurrent())
        {
            var result = new ArrayList<Patient>();
            var patientIds = new ArrayList<String>();

            var identifier = new IdentifierRecord(resourceIdentifier.getSystem(), resourceIdentifier.getValue());
            var identifierJson = objectMapper.writeValueAsString(new IdentifierRecord[] { identifier });

            var records = dsl.select(PATIENT)
                .from(PATIENT)
                .where(PATIENT.IDENTIFIERS.contains(JSONB.jsonb(identifierJson)))
                .fetch();
            for (var record : records)
            {
                var resource = resourceFromRecord(record.value1());
                result.add(resource);
                patientIds.add(resource.getIdPart());
            }

            span.setAttribute("audit.action", "search");
            span.setAttribute(AttributeKey.stringArrayKey("audit.patient"), patientIds);

            return result;
        }
        catch (Throwable t)
        {
            span.recordException(t);
            throw t;
        }
        finally
        {
            span.end();
        }
    }

    @Search
    public List<Patient> searchByLastUpdated(
        @RequiredParam(name = Constants.PARAM_LASTUPDATED) DateRangeParam dateRangeParam,
        @Sort SortSpec sort)
        throws JsonProcessingException
    {
        var span = tracer.spanBuilder("PatientProvider.searchByLastUpdated").startSpan();

        try (var scope = span.makeCurrent())
        {
            var patients = new ArrayList<Patient>();
            var patientIds = new ArrayList<String>();

            var query = dsl.select(PATIENT)
                .from(PATIENT)
                .where(conditionFromDateParam(dateRangeParam.getLowerBound()))
                .and(conditionFromDateParam(dateRangeParam.getUpperBound()));
            
            Result<Record1<PatientRecord>> result;

            if (sort != null)
            {
                if (!Constants.PARAM_LASTUPDATED.equals(sort.getParamName()) || sort.getChain() != null)
                {
                    logger.debug("Tried to sort by {}", sort.getParamName());
                    throw new InvalidRequestException("Can only sort by lastUpdated");
                }

                if (SortOrderEnum.DESC.equals(sort.getOrder()))
                {
                    result = query.orderBy(PATIENT.LAST_UPDATED.desc()).fetch();
                }
                else
                {
                    result = query.orderBy(PATIENT.LAST_UPDATED).fetch();
                }
            }
            else
            {
                result = query.fetch();
            }

            for (var record : result)
            {
                var resource = resourceFromRecord(record.value1());
                patients.add(resource);
                patientIds.add(resource.getIdPart());
            }

            span.setAttribute("audit.action", "search");
            span.setAttribute(AttributeKey.stringArrayKey("audit.patient"), patientIds);

            return patients;
        }
        catch (Throwable t)
        {
            span.recordException(t);
            throw t;
        }
        finally
        {
            span.end();
        }
    }

    @Operation(name = "$merge", idempotent = false)
    public Parameters merge(@OperationParam(name = "source-patient", min = 1, max = 1) Reference sourceReference,
        @OperationParam(name = "target-patient", min = 1, max = 1) Reference targetReference)
    {
        var span = tracer.spanBuilder("PatientProvider.merge").startSpan();

        try (var scope = span.makeCurrent())
        {
            var parameters = new Parameters()
                .addParameter("source-patient", sourceReference)
                .addParameter("target-patient", targetReference);

            try
            {
                var sourceIdType = sourceReference.getReferenceElement();
                var targetIdType = targetReference.getReferenceElement();

                if (sourceIdType == null || targetIdType == null)
                {
                    throw new InvalidRequestException("Needs source and target ids");
                }
                else if (sourceIdType.hasBaseUrl() || targetIdType.hasBaseUrl())
                {
                    throw new InvalidRequestException("Cannot handle absolute references");
                }
                else if (!"Patient".equals(sourceIdType.getResourceType())
                    || !"Patient".equals(targetIdType.getResourceType()))
                {
                    throw new InvalidRequestException("Source and target must be Patient");
                }

                var sourceId = sourceIdType.getIdPart();
                var targetId = targetIdType.getIdPart();

                var record = dsl.transactionResult(trx -> {
                    var targetFetch = trx.dsl().select(PATIENT)
                        .from(PATIENT)
                        .where(PATIENT.ID.equal(targetId))
                        .fetchAny();
                    if (targetFetch == null)
                    {
                        throw new InvalidRequestException("Target does not exist");
                    }
                    
                    var targetRecord = targetFetch.value1();
                    if (targetRecord.getMergedInto() != null)
                    {
                        throw new InvalidRequestException("Target is already merged");
                    }

                    var sourceFetch = trx.dsl().select(PATIENT.MERGED_INTO)
                        .from(PATIENT)
                        .where(PATIENT.ID.equal(sourceId))
                        .fetchAny();
                    if (sourceFetch == null)
                    {
                        throw new InvalidRequestException("Source does not exist");
                    }

                    if (sourceFetch.value1() != null)
                    {
                        throw new InvalidRequestException("Source is already merged");
                    }

                    trx.dsl().update(PATIENT)
                        .set(PATIENT.VERSION_ID, PATIENT.VERSION_ID.add(1))
                        .set(PATIENT.LAST_UPDATED, DSL.currentOffsetDateTime())
                        .set(PATIENT.MERGED_INTO, targetId)
                        .where(PATIENT.ID.equal(sourceId))
                        .execute();

                    return targetRecord;
                });

                var resource = resourceFromRecord(record);

                parameters.addParameter().setName("outcome").setResource(
                    new OperationOutcome(new OperationOutcomeIssueComponent(IssueSeverity.SUCCESS, IssueType.SUCCESS)));
                parameters.addParameter().setName("result").setResource(resource);

                span.setAttribute("audit.action", "merge");
                span.setAttribute("audit.patient.soure", sourceId);
                span.setAttribute("audit.patient.target", targetId);
            }
            catch (Exception e)
            {
                // TODO Write message
                parameters.addParameter().setName("outcome").setResource(
                    new OperationOutcome(new OperationOutcomeIssueComponent(IssueSeverity.ERROR, IssueType.PROCESSING)));
            }

            return parameters;
        }
        catch (Throwable t)
        {
            span.recordException(t);
            throw t;
        }
        finally
        {
            span.end();
        }
    }

    private MethodOutcome insertOrUpdatePatient(@Nullable String id, Patient patient)
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

        var result = dsl.transactionResult(trx -> {
            var resourceId = id;

            Boolean created;
            PatientRecord patientRecord;

            if (resourceId == null)
            {
                do
                {
                    resourceId = UUID.randomUUID().toString();
                }
                while (trx.dsl().selectOne().from(PATIENT)
                    .where(PATIENT.ID.equal(resourceId)).fetchAny() != null);
                
                created = Boolean.TRUE;
            }
            else
            {
                var mergedIntoRecord = trx.dsl()
                    .select(PATIENT.MERGED_INTO)
                    .from(PATIENT)
                    .where(PATIENT.ID.equal(resourceId))
                    .fetchAny();
                
                if (mergedIntoRecord == null)
                {
                    created = Boolean.TRUE;
                }
                else if (mergedIntoRecord.value1() == null)
                {
                    created = Boolean.FALSE;
                }
                else
                {
                    throw new InvalidRequestException("Cannot update merged resource");
                }
            }

            if (created.booleanValue())
            {
                patientRecord = trx.dsl().insertInto(PATIENT)
                    .set(PATIENT.ID, resourceId)
                    .set(PATIENT.VERSION_ID, 1)
                    .set(PATIENT.LAST_UPDATED, DSL.currentOffsetDateTime())
                    .set(PATIENT.IDENTIFIERS, JSONB.jsonb(identifierJson))
                    .returningResult(PATIENT)
                    .fetchAny().value1();
            }
            else
            {
                patientRecord = trx.dsl().update(PATIENT)
                    .set(PATIENT.VERSION_ID, PATIENT.VERSION_ID.add(1))
                    .set(PATIENT.LAST_UPDATED, DSL.currentOffsetDateTime())
                    .set(PATIENT.IDENTIFIERS, JSONB.jsonb(identifierJson))
                    .where(PATIENT.ID.equal(resourceId))
                    .returningResult(PATIENT)
                    .fetchAny().value1();
            }

            return new CreateOrUpdateResult(created, patientRecord);
        });

        var resource = resourceFromRecord(result.record());

        var outcome = new MethodOutcome(resource.getIdElement(), result.created());
        outcome.setResource(resource);

        return outcome;
    }

    private Condition conditionFromDateParam(@Nullable DateParam param)
    {
        Condition condition;

        if (param != null)
        {
            var date = param.getValue();

            if (date != null)
            {
                var dateTime = OffsetDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());

                var prefix = param.getPrefix();

                if (prefix != null)
                {
                    switch (prefix)
                    {
                    case GREATERTHAN:
                        condition = PATIENT.LAST_UPDATED.greaterThan(dateTime);
                        break;
                    case GREATERTHAN_OR_EQUALS:
                        condition = PATIENT.LAST_UPDATED.greaterOrEqual(dateTime);
                        break;
                    case LESSTHAN:
                        condition = PATIENT.LAST_UPDATED.lessThan(dateTime);
                        break;
                    case LESSTHAN_OR_EQUALS:
                        condition = PATIENT.LAST_UPDATED.lessOrEqual(dateTime);
                        break;
                    case NOT_EQUAL:
                        condition = PATIENT.LAST_UPDATED.equal(dateTime);
                        break;
                    case STARTS_AFTER:
                    case ENDS_BEFORE:
                        throw new InvalidRequestException("Unsupported prefix");
                    case APPROXIMATE:
                    case EQUAL:
                    default:
                        condition = PATIENT.LAST_UPDATED.equal(dateTime);
                        break;
                    }
                }
                else
                {
                    condition = PATIENT.LAST_UPDATED.equal(dateTime);
                }
            }
            else
            {
                condition = DSL.trueCondition();
            }
        }
        else
        {
            condition = DSL.trueCondition();
        }

        return condition;
    }

    private Patient resourceFromRecord(PatientRecord record) throws JsonProcessingException
    {
        var resource = new Patient();

        resource.setIdElement(new IdType("Patient", record.getId()));

        resource.setMeta(new Meta()
            .setLastUpdated(Date.from(record.getLastUpdated().toInstant()))
            .setVersionId(record.getVersionId().toString())
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

    @JsonInclude(Include.NON_EMPTY)
    private record IdentifierRecord(String system, String value) { }

    private record CreateOrUpdateResult(Boolean created, PatientRecord record) { }
}
