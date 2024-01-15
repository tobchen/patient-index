package de.tobchen.health.patientindex.model.repositories;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import de.tobchen.health.patientindex.model.entities.PatientEntity;

public interface PatientRepository extends Repository<PatientEntity, Long>
{
    public PatientEntity save(PatientEntity entity);

    public Optional<PatientEntity> findByResourceId(String resourceId);

    public Iterable<PatientEntity> findByIdentifiers_SystemAndIdentifiers_Val(String system, String value);
    
    public Iterable<PatientEntity> findByIdentifiers_System(String system);

    public Iterable<PatientEntity> findByIdentifiers_Val(String value);

    public Iterable<PatientEntity> findByUpdatedAtGreaterThan(Instant instant);

    public Iterable<PatientEntity> findByUpdatedAtLessThanEqual(Instant instant);

    public Iterable<PatientEntity> findByUpdatedAtGreaterThanAndUpdatedAtLessThanEqual(Instant from, Instant to);

    public boolean existsByResourceId(String resourceId);
}
