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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public MethodOutcome create(@ResourceParam Patient patient, HttpServletRequest request)
    {
        var outcome = createOrUpdate(null, patient);

        audit(RestOperationTypeEnum.CREATE, AuditEventAction.C,
            List.of((Patient) outcome.getResource()), request);

        return outcome;
    }

    @Update
    synchronized public MethodOutcome update(@IdParam IIdType resourceId,
        @ResourceParam Patient patient, HttpServletRequest request)
    {
        var outcome = createOrUpdate(resourceId.getIdPart(), patient);

        audit(RestOperationTypeEnum.UPDATE,
            outcome.getCreated().booleanValue() ? AuditEventAction.C : AuditEventAction.U,
            List.of((Patient) outcome.getResource()), request);

        return outcome;
    }

    @Read
    @Transactional(readOnly = true)
    public Patient read(@IdParam IIdType resourceId, HttpServletRequest request)
    {
        // TODO Audit Event

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
        HttpServletRequest request)
    {
        // TODO Audit Event
        
        var result = new ArrayList<Patient>();

        String system = resourceIdentifier.getSystem();
        String value = resourceIdentifier.getValue();

        if (system != null && value != null)
        {
            for (var entity : repository.findByIdentifiers_SystemAndIdentifiers_Val(system, value))
            {
                result.add(resourceFromEntity(entity));
            }
        }

        audit(RestOperationTypeEnum.SEARCH_TYPE, AuditEventAction.E, result, request);

        return result;
    }

    @Transactional
    private MethodOutcome createOrUpdate(String resourceId, Patient patient)
    {
        PatientEntity entity = null;
        boolean wasCreated = false;

        if (resourceId != null)
        {
            var optionalEntity = repository.findByResourceId(resourceId);
            if (optionalEntity.isPresent())
            {
                entity = optionalEntity.get();
            }
        }

        if (entity == null)
        {
            if (resourceId == null)
            {
                do
                {
                    resourceId = UUID.randomUUID().toString();
                }
                while (repository.existsByResourceId(resourceId));
            }

            entity = new PatientEntity(resourceId);
            wasCreated = true;
        }

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

        var outcome = new MethodOutcome(new IdType("Patient", resourceId),
            Boolean.valueOf(wasCreated));
        outcome.setResource(resourceFromEntity(entity));

        return outcome;
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
        Iterable<Patient> patients, HttpServletRequest request)
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
