package de.tobchen.health.patientindex.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Meta;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.AuditEvent.AuditEventAction;
import org.hl7.fhir.r5.model.Patient.LinkType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.uhn.fhir.rest.annotation.ConditionalUrlParam;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceVersionConflictException;
import ca.uhn.fhir.util.UrlUtil;
import de.tobchen.health.patientindex.model.dto.AuditDto;
import de.tobchen.health.patientindex.model.embeddables.IdentifierEmbeddable;
import de.tobchen.health.patientindex.model.entities.PatientEntity;
import de.tobchen.health.patientindex.model.repositories.PatientRepository;

@Service
public class PatientProvider implements IResourceProvider
{
    private final ApplicationEventPublisher publisher;
    private final PatientRepository repository;

    public PatientProvider(ApplicationEventPublisher publisher, PatientRepository repository)
    {
        this.publisher = publisher;
        this.repository = repository;
    }

    @Override
    public Class<? extends IBaseResource> getResourceType()
    {
        return Patient.class;
    }

    @Create
    public MethodOutcome create(@ResourceParam Patient patient, @Nullable HttpServletRequest request)
    {
        var outcome = create(patient);

        audit(RestOperationTypeEnum.CREATE, AuditEventAction.C,
            List.of((Patient) outcome.getResource()), request);

        return outcome;
    }

    @Update
    synchronized public MethodOutcome update(@Nullable @IdParam IIdType idType,
        @Nullable @ConditionalUrlParam String conditional, @ResourceParam Patient patient,
        @Nullable HttpServletRequest request)
    {
        MethodOutcome outcome;

        if (idType != null)
        {
            outcome = updateOrUpdateAsCreate(idType.getIdPart(), patient);
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

        audit(RestOperationTypeEnum.UPDATE,
            outcome.getCreated().booleanValue() ? AuditEventAction.C : AuditEventAction.U,
            List.of((Patient) outcome.getResource()), request);

        return outcome;
    }

    @Read
    @Transactional(readOnly = true)
    public Patient read(@IdParam IIdType resourceId, @Nullable HttpServletRequest request)
    {
        Patient resource = null;

        var optionalEntity = repository.findByResourceId(resourceId.getIdPart());
        if (optionalEntity.isPresent())
        {
            resource = resourceFromEntity(optionalEntity.get());
        }

        audit(RestOperationTypeEnum.READ, AuditEventAction.R, List.of(resource), request);

        return resource;
    }

    @Search
    @Transactional(readOnly = true)
    public List<Patient> searchByIdentifier(
        @RequiredParam(name = Patient.SP_IDENTIFIER) TokenParam resourceIdentifier,
        @Nullable HttpServletRequest request)
    {
        var result = new ArrayList<Patient>();

        for (var entity : findBySystemAndValue(
            resourceIdentifier.getSystem(), resourceIdentifier.getValue()))
        {
            result.add(resourceFromEntity(entity));
        }

        audit(RestOperationTypeEnum.SEARCH_TYPE, AuditEventAction.E, result, request);

        return result;
    }

    @Transactional
    private MethodOutcome create(Patient patient)
    {
        return createAndSaveEntity(patient);
    }

    @Transactional
    private MethodOutcome conditionalUpdate(@Nullable String system, @Nullable String value,
        Patient patient)
    {
        MethodOutcome outcome;

        String resourceId = patient.getIdPart();

        var entities = new ArrayList<PatientEntity>();
        findBySystemAndValue(system, value).forEach(entities::add);;
        
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
            if (resourceId == null || resourceId.equals(entity.getResourceId()))
            {
                outcome = updateAndSaveEntity(entity, patient);
            }
            else
            {
                throw new InvalidRequestException(
                    "One Match, resource id provided but does not match resource found");
            }
        }
        else
        {
            throw new PreconditionFailedException("Multiple matches");
        }

        return outcome;
    }

    @Transactional
    private MethodOutcome updateOrUpdateAsCreate(String resourceId, Patient patient)
    {
        MethodOutcome outcome;

        var optionalEntity = repository.findByResourceId(resourceId);
        if (optionalEntity.isPresent())
        {
            outcome = updateAndSaveEntity(optionalEntity.get(), patient);
        }
        else
        {
            outcome = createAndSaveEntity(resourceId, patient);
        }

        return outcome;
    }

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

    private MethodOutcome createAndSaveEntity(String resourceId, Patient patient)
    {
        var entity = new PatientEntity(resourceId);

        return updateAndSaveEntity(entity, patient).setCreated(Boolean.TRUE);
    }

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

        entity = repository.save(entity);

        return new MethodOutcome(
            new IdType("Patient", entity.getResourceId()), Boolean.FALSE)
            .setResource(resourceFromEntity(entity));
    }

    private Iterable<PatientEntity> findBySystemAndValue(@Nullable String system, @Nullable String value)
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
            result = List.of();
        }

        return result;
    }

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

    private void audit(RestOperationTypeEnum operation, AuditEventAction action,
        Iterable<Patient> patients, @Nullable HttpServletRequest request)
    {
        var recordedAt = new Date();

        String sourceAddress = null;
        String destinationAddress = null;
        String query = null;

        if (request != null)
        {
            sourceAddress = request.getRemoteAddr();
            destinationAddress = request.getLocalAddr();
            
            if (operation == RestOperationTypeEnum.SEARCH_TYPE)
            {
                query = request.getRequestURL().toString();
            }
        }

        for (var patient : patients)
        {
            publisher.publishEvent(
                new AuditDto("http://terminology.hl7.org/CodeSystem/audit-event-type",
                "rest", "http://hl7.org/fhir/restful-interaction",
                operation.getCode(), action, recordedAt, patient.getIdPart(),
                "FHIR Client", sourceAddress, "FHIR Server",
                destinationAddress, query));
        }
    }
}
