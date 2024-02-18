package de.tobchen.health.patientindex.model.repositories;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import de.tobchen.health.patientindex.model.entities.PatientEntity;

public interface PatientRepository extends Repository<PatientEntity, Long>
{
    public PatientEntity save(PatientEntity entity);

    public PatientEntity saveAndFlush(PatientEntity entity);

    public Optional<PatientEntity> findByResourceId(String resourceId);

    public Iterable<PatientEntity> findByIdentifiers_SystemAndIdentifiers_Val(String system, String value);
    
    public Iterable<PatientEntity> findByIdentifiers_System(String system);

    public Iterable<PatientEntity> findByIdentifiers_Val(String value);

    public Iterable<PatientEntity> findByUpdatedAtGreaterThan(Instant instant);

    public Iterable<PatientEntity> findByUpdatedAtGreaterThanEqual(Instant instant);

    public boolean existsByResourceId(String resourceId);
}
