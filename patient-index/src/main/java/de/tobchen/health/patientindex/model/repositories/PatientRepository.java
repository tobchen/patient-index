package de.tobchen.health.patientindex.model.repositories;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import de.tobchen.health.patientindex.model.entities.PatientEntity;

public interface PatientRepository extends Repository<PatientEntity, Long>
{
    public PatientEntity save(PatientEntity entity);

    public Optional<PatientEntity> findByResourceId(String resourceId);

    public Iterable<PatientEntity> findByIdentifiers_SystemAndIdentifiers_Val(
        String system, String value);

    public boolean existsByResourceId(String resourceId);
}
