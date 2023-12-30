package de.tobchen.health.patientindex.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.Meta;
import org.hl7.fhir.r5.model.Patient;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.Patient.LinkType;
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
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import de.tobchen.health.patientindex.model.embeddables.IdentifierEmbeddable;
import de.tobchen.health.patientindex.model.entities.PatientEntity;
import de.tobchen.health.patientindex.model.repositories.PatientRepository;

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
    public MethodOutcome create(@ResourceParam Patient patient)
    {
        // TODO Audit Event

        return createOrUpdate(null, patient);
    }

    @Update
    @Transactional
    synchronized public MethodOutcome createOrUpdate(@IdParam IIdType resourceId,
        @ResourceParam Patient patient)
    {
        // TODO Audit Event

        String id = null;
        PatientEntity entity = null;
        boolean wasCreated = false;

        if (resourceId != null)
        {
            id = resourceId.getIdPart();

            var optionalEntity = repository.findByResourceId(id);
            if (optionalEntity.isPresent())
            {
                entity = optionalEntity.get();
            }
        }

        if (entity == null)
        {
            if (id == null)
            {
                do
                {
                    id = UUID.randomUUID().toString();
                }
                while (repository.existsByResourceId(id));
            }

            entity = new PatientEntity(id);
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

        repository.save(entity);

        if (wasCreated)
        {
            // TODO Create Event
        }

        return new MethodOutcome(new IdType("Patient", id), Boolean.valueOf(wasCreated));
    }

    @Read
    @Transactional(readOnly = true)
    public Patient read(@IdParam IIdType resourceId)
    {
        // TODO Audit Event

        Patient resource = null;

        var optionalEntity = repository.findByResourceId(resourceId.getIdPart());
        if (optionalEntity.isPresent())
        {
            resource = resourceFromEntity(optionalEntity.get());
        }

        return resource;
    }

    @Search
    @Transactional(readOnly = true)
    public List<Patient> searchByIdentifier(
        @RequiredParam(name = Patient.SP_IDENTIFIER) TokenParam resourceIdentifier)
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
}
