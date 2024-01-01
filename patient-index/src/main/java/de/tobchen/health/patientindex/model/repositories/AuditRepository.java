package de.tobchen.health.patientindex.model.repositories;

import org.springframework.data.repository.Repository;

import de.tobchen.health.patientindex.model.entities.AuditEntity;

public interface AuditRepository extends Repository<AuditEntity, Long>
{
    public AuditEntity save(AuditEntity entity);
    
    public Iterable<AuditEntity> saveAll(Iterable<AuditEntity> entities);

    public Iterable<AuditEntity> findAll();

    public Iterable<AuditEntity> findByPatient_ResourceId(String resourceId);
}
