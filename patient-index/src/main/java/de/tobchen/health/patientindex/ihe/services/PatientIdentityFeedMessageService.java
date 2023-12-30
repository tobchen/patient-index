package de.tobchen.health.patientindex.ihe.services;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.tobchen.health.patientindex.ihe.model.entities.PatientIdentityFeedMessageEntity;
import de.tobchen.health.patientindex.ihe.model.enums.MessageStatus;
import de.tobchen.health.patientindex.ihe.model.records.PatientIdentityFeedMessageRecord;
import de.tobchen.health.patientindex.ihe.model.repositories.PatientIdentityFeedMessageRepository;

@Service
public class PatientIdentityFeedMessageService
{
    private final PatientIdentityFeedMessageRepository repository;

    public PatientIdentityFeedMessageService(PatientIdentityFeedMessageRepository repository)
    {
        this.repository = repository;
    }

    @Transactional
    public Long create(String message)
    {
        var entity = new PatientIdentityFeedMessageEntity(message);

        return repository.save(entity).getId();
    }

    @Transactional
    public void updateToSent(Long id, String response)
    {
        var optionalEntity = repository.findById(id);
        if (optionalEntity.isPresent())
        {
            var entity = optionalEntity.get();

            entity.setResponse(response);
            entity.setStatus(MessageStatus.SENT);

            repository.save(entity);
        }
    }

    @Transactional(readOnly = true)
    public Optional<PatientIdentityFeedMessageRecord> read(Long id)
    {
        Optional<PatientIdentityFeedMessageRecord> record;
        
        var optionalEntity = repository.findById(id);
        if (optionalEntity.isPresent())
        {
            record = Optional.of(recordFromEntity(optionalEntity.get()));
        }
        else
        {
            record = Optional.empty();
        }

        return record;
    }

    private PatientIdentityFeedMessageRecord recordFromEntity(PatientIdentityFeedMessageEntity entity)
    {
        return new PatientIdentityFeedMessageRecord(entity.getId(), entity.getStatus(),
            entity.getMessage(), entity.getResponse());
    }
}
