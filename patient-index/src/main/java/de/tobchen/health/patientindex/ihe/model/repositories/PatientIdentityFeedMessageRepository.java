package de.tobchen.health.patientindex.ihe.model.repositories;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import de.tobchen.health.patientindex.ihe.model.entities.PatientIdentityFeedMessageEntity;

public interface PatientIdentityFeedMessageRepository extends Repository<PatientIdentityFeedMessageEntity, Long>
{
    public PatientIdentityFeedMessageEntity save(PatientIdentityFeedMessageEntity entity);

    public Optional<PatientIdentityFeedMessageEntity> findById(Long id);
}
