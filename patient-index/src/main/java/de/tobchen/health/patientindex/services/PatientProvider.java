package de.tobchen.health.patientindex.services;

import java.time.Instant;
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
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.annotation.ConditionalUrlParam;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceVersionConflictException;
import ca.uhn.fhir.util.UrlUtil;
import de.tobchen.health.patientindex.model.embeddables.IdentifierEmbeddable;
import de.tobchen.health.patientindex.model.entities.PatientEntity;
import de.tobchen.health.patientindex.model.repositories.PatientRepository;
import io.opentelemetry.instrumentation.annotations.WithSpan;

@Service
public class PatientProvider implements IResourceProvider
{
    private final PatientRepository repository;

    public PatientProvider(PatientRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public Class<? extends IBaseResource> getResourceType()
    {
        return Patient.class;
    }

    @Create
    @WithSpan
    public MethodOutcome create(@ResourceParam Patient patient)
    {
        return createAndSaveEntity(patient);
    }

    @Update
    @WithSpan
    public MethodOutcome update(@Nullable @IdParam IIdType idType,
        @Nullable @ConditionalUrlParam String conditional, @ResourceParam Patient patient)
    {
        MethodOutcome outcome;

        if (idType != null)
        {
            var idPart = idType.getIdPart();
            if (idPart != null)
            {
                outcome = updateOrUpdateAsCreate(idPart, patient);
            }
            else
            {
                throw new InvalidRequestException("Id is missing id part");
            }
        }
        else if (conditional != null)
        {
            var parts = UrlUtil.parseUrl(conditional);

            var queries = UrlUtil.parseQueryString(parts.getParams());
            if (queries.size() != 1)
            {
                throw new InvalidRequestException("Unequal one search parameters");
            }

            var identifierQuery = queries.get("identifier");
            if (identifierQuery == null || identifierQuery.length != 1)
            {
                throw new InvalidRequestException("Conditional update accepts only one identifier query");
            }

            var systemAndValue = identifierQuery[0].split("\\|");

            outcome = conditionalUpdate(systemAndValue.length > 0 ? systemAndValue[0] : null,
                systemAndValue.length > 1 ? systemAndValue[1] : null, patient);
        }
        else
        {
            throw new InvalidRequestException("Both id and conditional are null");
        }

        return outcome;
    }

    @Read
    @WithSpan
    @Transactional(readOnly = true)
    public @Nullable Patient read(@IdParam IIdType resourceId)
    {
        Patient resource = null;

        var idPart = resourceId.getIdPart();
        if (idPart != null)
        {
            var entity = repository.findByResourceId(idPart).orElse(null);
            if (entity != null)
            {
                resource = resourceFromEntity(entity);
            }
        }

        return resource;
    }

    @Search
    @WithSpan
    @Transactional(readOnly = true)
    public List<Patient> searchByIdentifier(
        @RequiredParam(name = Patient.SP_IDENTIFIER) TokenParam resourceIdentifier)
    {
        var result = new ArrayList<Patient>();

        var entities = findBySystemAndValue(resourceIdentifier.getSystem(), resourceIdentifier.getValue());
        if (entities != null)
        {
            for (var entity : entities)
            {
                if (entity != null)
                {
                    result.add(resourceFromEntity(entity));
                }
            }
        }

        return result;
    }

    @Search
    @WithSpan
    @Transactional(readOnly = true)
    public List<Patient> searchByLastUpdated(
        @RequiredParam(name = Constants.PARAM_LASTUPDATED) DateRangeParam dateRangeParam)
    {
        Instant startInstant;
        var startParam = dateRangeParam.getLowerBound();
        if (startParam != null)
        {
            if (!ParamPrefixEnum.GREATERTHAN.equals(startParam.getPrefix()))
            {
                throw new InvalidRequestException("Only gt prefix for start date supported");
            }
            else if (!TemporalPrecisionEnum.MILLI.equals(startParam.getPrecision()))
            {
                throw new InvalidRequestException("Only full precision supported");
            }

            startInstant = startParam.getValue().toInstant();
        }
        else
        {
            startInstant = null;
        }

        Instant endInstant;
        var endParam = dateRangeParam.getUpperBound();
        if (endParam != null)
        {
            if (!ParamPrefixEnum.LESSTHAN_OR_EQUALS.equals(endParam.getPrefix()))
            {
                throw new InvalidRequestException("Only le prefix for end date supported");
            }
            else if (!TemporalPrecisionEnum.MILLI.equals(endParam.getPrecision()))
            {
                throw new InvalidRequestException("Only full precision supported");
            }

            endInstant = endParam.getValue().toInstant();
        }
        else
        {
            endInstant = null;
        }

        var result = new ArrayList<Patient>();

        Iterable<PatientEntity> entities = null;

        if (startInstant != null)
        {
            if (endInstant != null)
            {
                if (startInstant != null && endInstant != null)
                {
                    entities =
                        repository.findByUpdatedAtGreaterThanAndUpdatedAtLessThanEqual(startInstant, endInstant);
                }
            }
            else
            {
                entities = repository.findByUpdatedAtGreaterThan(startInstant);
            }
        }
        else if (endInstant != null)
        {
            entities = repository.findByUpdatedAtLessThanEqual(endInstant);
        }

        if (entities != null)
        {
            for (var entity : entities)
            {
                if (entity != null)
                {
                    result.add(resourceFromEntity(entity));
                }
            }
        }

        return result;
    }

    @Operation(name = "$merge", idempotent = false)
    @WithSpan
    public Parameters merge(@OperationParam(name = "source-patient", min = 1, max = 1) Reference sourceReference,
        @OperationParam(name = "target-patient", min = 1, max = 1) Reference targetReference)
    {
        var parameters = new Parameters()
            .addParameter("source-patient", sourceReference)
            .addParameter("target-patient", targetReference);

        try
        {
            var sourceId = idFromReference(sourceReference, "Patient", "source-patient");
            var targetId = idFromReference(targetReference, "Patient", "target-patient");

            var patient = merge(sourceId, targetId);

            parameters.addParameter().setName("outcome").setResource(
                new OperationOutcome(new OperationOutcomeIssueComponent(IssueSeverity.SUCCESS, IssueType.SUCCESS)));
            parameters.addParameter().setName("result").setResource(patient);
        }
        catch (Exception e)
        {
            parameters.addParameter().setName("outcome").setResource(
                new OperationOutcome(new OperationOutcomeIssueComponent(IssueSeverity.ERROR, IssueType.PROCESSING)));
        }

        return parameters;
    }

    @WithSpan
    @Transactional
    private MethodOutcome conditionalUpdate(@Nullable String system, @Nullable String value,
        Patient patient)
    {
        MethodOutcome outcome;

        String resourceId = patient.getIdPart();

        var entities = new ArrayList<PatientEntity>();

        var foundEntities = findBySystemAndValue(system, value);
        if (foundEntities != null)
        {
            foundEntities.forEach(entities::add);
        }
        
        // https://hl7.org/fhir/http.html#cond-update
        var entityCount = entities.size();
        if (entityCount == 0)
        {
            if (resourceId == null)
            {
                outcome = createAndSaveEntity(patient);
            }
            else
            {
                if (!repository.existsByResourceId(resourceId))
                {
                    outcome = createAndSaveEntity(resourceId, patient);
                }
                else
                {
                    throw new ResourceVersionConflictException("No matches, id provided and already exist");
                }
            }
        }
        else if (entityCount == 1)
        {
            var entity = entities.get(0);
            if (entity == null)
            {
                throw new InternalErrorException("One match, but is null");
            }
            else if (resourceId == null || resourceId.equals(entity.getResourceId()))
            {
                outcome = updateAndSaveEntity(entity, patient);
            }
            else
            {
                throw new InvalidRequestException(
                    "One match, resource id provided but does not match resource found");
            }
        }
        else
        {
            throw new PreconditionFailedException("Multiple matches");
        }

        return outcome;
    }

    @WithSpan
    @Transactional
    private MethodOutcome updateOrUpdateAsCreate(String resourceId, Patient patient)
    {
        MethodOutcome outcome;

        var entity = repository.findByResourceId(resourceId).orElse(null);
        if (entity != null)
        {
            outcome = updateAndSaveEntity(entity, patient);
        }
        else
        {
            outcome = createAndSaveEntity(resourceId, patient);
        }

        return outcome;
    }

    @WithSpan
    @Transactional
    private MethodOutcome createAndSaveEntity(Patient patient)
    {
        String resourceId;
        do
        {
            resourceId = UUID.randomUUID().toString();
        }
        while (repository.existsByResourceId(resourceId));

        return createAndSaveEntity(resourceId, patient);
    }

    @WithSpan
    @Transactional
    private MethodOutcome createAndSaveEntity(String resourceId, Patient patient)
    {
        var entity = new PatientEntity(resourceId);

        var outcome = updateAndSaveEntity(entity, patient);
        outcome.setCreated(Boolean.TRUE);

        return outcome;
    }

    @WithSpan
    private MethodOutcome updateAndSaveEntity(PatientEntity entity, Patient patient)
    {
        var entityIdentifiers = entity.getIdentifiers();
        entityIdentifiers.clear();

        var patientIdentifiers = patient.getIdentifier();
        if (patientIdentifiers != null)
        {
            for (var patientIdentifier : patientIdentifiers)
            {
                var value = patientIdentifier.getValue();
                if (value != null)
                {
                    var system = patientIdentifier.getSystem();
                    if (system != null)
                    {
                        entityIdentifiers.add(new IdentifierEmbeddable(system, value));
                    }
                }
            }
        }

        entity = save(entity);

        var outcome = new MethodOutcome(
            new IdType("Patient", entity.getResourceId()), Boolean.FALSE);
        outcome.setResource(resourceFromEntity(entity));

        return outcome;
    }

    @WithSpan
    @Transactional(readOnly = true)
    private @Nullable Iterable<PatientEntity> findBySystemAndValue(@Nullable String system, @Nullable String value)
    {
        Iterable<PatientEntity> result;

        if (system != null && !system.isEmpty())
        {
            if (value != null && !value.isEmpty())
            {
                result = repository.findByIdentifiers_SystemAndIdentifiers_Val(system, value);
            }
            else
            {
                result = repository.findByIdentifiers_System(system);
            }
        }
        else if (value != null && !value.isEmpty())
        {
            result = repository.findByIdentifiers_Val(value);
        }
        else
        {
            result = null;
        }

        return result;
    }

    @WithSpan
    private Patient resourceFromEntity(PatientEntity entity)
    {
        var resource = new Patient();

        resource.setId(entity.getResourceId());

        var meta = new Meta();
        meta.setLastUpdated(Date.from(entity.getUpdatedAt()));
        resource.setMeta(meta);
        
        for (var identifier : entity.getIdentifiers())
        {
            resource.addIdentifier().setSystem(identifier.getSystem()).setValue(identifier.getVal());
        }

        var mergedInto = entity.getMergedInto();
        if (mergedInto != null)
        {
            resource.setActive(false);

            resource.addLink()
                .setOther(new Reference(new IdType("Patient", mergedInto.getId())))
                .setType(LinkType.REPLACEDBY);
        }
        else
        {
            resource.setActive(true);
        }

        return resource;
    }

    @WithSpan
    private String idFromReference(Reference reference, String resourceType, String name)
    {
        var literalReference = reference.getReference();
        if (literalReference == null)
        {
            throw new InvalidRequestException("Missing " + name + " literal reference");
        }

        if (UrlUtil.isAbsolute(literalReference))
        {
            throw new InvalidRequestException(name + " must be relative literal reference");
        }

        var parts = UrlUtil.parseUrl(literalReference);
        if (parts == null || !resourceType.equals(parts.getResourceType()))
        {
            throw new InvalidRequestException(name + " must be reference of type " + resourceType);
        }

        var resourceId = parts.getResourceId();
        if (resourceId == null)
        {
            throw new InvalidRequestException("Resource id missing for " + name);
        }

        return resourceId;
    }

    @WithSpan
    @Transactional
    private Patient merge(String sourceId, String targetId)
    {
        PatientEntity sourceEntity = repository.findByResourceId(sourceId).orElse(null);
        PatientEntity targetEntity = repository.findByResourceId(targetId).orElse(null);

        if (sourceEntity == null)
        {
            throw new InvalidRequestException("Source patient not found");
        }
        else if (targetEntity == null)
        {
            throw new InvalidRequestException("Target patient not found");
        }
        else if (sourceEntity.getId().equals(targetEntity.getId()))
        {
            throw new InvalidRequestException("Source and target patient are the same");
        }
        else if (sourceEntity.getMergedInto() != null)
        {
            throw new InvalidRequestException("Source patient is already merged");
        }
        else if (targetEntity.getMergedInto() != null)
        {
            throw new InvalidRequestException("Target patient is already merged");
        }

        sourceEntity.setMergedInto(targetEntity);

        save(sourceEntity);

        return resourceFromEntity(targetEntity);
    }

    private PatientEntity save(PatientEntity entity)
    {
        entity.setUpdatedAt();
        return repository.save(entity);
    }
}
