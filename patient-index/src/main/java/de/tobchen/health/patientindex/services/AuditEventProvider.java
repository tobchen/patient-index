package de.tobchen.health.patientindex.services;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4b.model.IdType;
import org.hl7.fhir.r5.model.AuditEvent;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.AuditEvent.AuditEventSourceComponent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import de.tobchen.health.patientindex.model.entities.AuditEntity;
import de.tobchen.health.patientindex.model.repositories.AuditRepository;

@Service
public class AuditEventProvider implements IResourceProvider
{
    private final AuditRepository repository;

    public AuditEventProvider(AuditRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public Class<AuditEvent> getResourceType()
    {
        return AuditEvent.class;
    }
    
    @Search
    @Transactional(readOnly = true)
    public List<AuditEvent> search(
        @OptionalParam(name = AuditEvent.SP_PATIENT) ReferenceParam patientReference)
    {
        var auditEvents = new ArrayList<AuditEvent>();

        if (patientReference != null && patientReference.getBaseUrl() == null
            && patientReference.getChain() == null
            && "Patient".equals(patientReference.getResourceType()))
        {
            for (var entity : repository.findByPatient_ResourceId(patientReference.getIdPart()))
            {
                auditEvents.add(resourceFromEntity(entity));
            }
        }
        else if (patientReference == null)
        {
            for (var entity : repository.findAll())
            {
                auditEvents.add(resourceFromEntity(entity));
            }
        }

        return auditEvents;
    }

    private AuditEvent resourceFromEntity(AuditEntity entity)
    {
        var resource = new AuditEvent();

        resource.setId(entity.getId().toString());

        resource.addCategory().addCoding()
            .setSystem(entity.getCategorySystem())
            .setCode(entity.getCategoryCode());
        
        resource.setCode(new CodeableConcept(new Coding()
            .setSystem(entity.getCodeSystem())
            .setCode(entity.getCodeCode())));

        resource.setAction(entity.getAction());

        resource.setRecorded(entity.getRecordedAt());

        resource.setPatient(new Reference(
            new IdType("Patient", entity.getPatient().getId())));
        
        var sourceAgent = resource.addAgent();
        sourceAgent.setWho(new Reference().setDisplay(entity.getSourceDisplay()));
        sourceAgent.setType(new CodeableConcept(
            new Coding("http://dicom.nema.org/resources/ontology/DCM",
            "110153", "Source Role ID")));
        var sourceAddress = entity.getSourceAddress();
        if (sourceAddress != null)
        {
            sourceAgent.setNetwork(new StringType(sourceAddress));
        }

        var destinationAgent = resource.addAgent();
        destinationAgent.setWho(new Reference().setDisplay(entity.getDestinationDisplay()));
        destinationAgent.setType(new CodeableConcept(
            new Coding("http://dicom.nema.org/resources/ontology/DCM",
            "110152", "Destination Role ID")));
        var destinationAddress = entity.getDestinationAddress();
        if (destinationAddress != null)
        {
            destinationAgent.setNetwork(new StringType(destinationAddress));
        }

        resource.setSource(new AuditEventSourceComponent(new Reference().setDisplay("Patient Index")));

        var query = entity.getQuery();
        if (query != null)
        {
            resource.addEntity()
                .setRole(new CodeableConcept(
                    new Coding("http://terminology.hl7.org/CodeSystem/object-role",
                    "24", "Query")))
                .setQuery(query.getBytes(StandardCharsets.UTF_8));
        }

        return resource;
    }
}
