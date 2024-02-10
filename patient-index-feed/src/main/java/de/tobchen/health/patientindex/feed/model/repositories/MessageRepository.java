package de.tobchen.health.patientindex.feed.model.repositories;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import de.tobchen.health.patientindex.feed.model.entities.MessageEntity;
import de.tobchen.health.patientindex.feed.model.enums.MessageStatus;
import de.tobchen.health.patientindex.feed.model.projections.MessagePatientIdAndVersionId;
import de.tobchen.health.patientindex.feed.model.projections.MessagePatientUpdatedAt;

public interface MessageRepository extends Repository<MessageEntity, Long>
{
    public MessageEntity save(MessageEntity entity);
    
    public Iterable<MessageEntity> saveAll(Iterable<MessageEntity> entities);

    public Optional<MessageEntity> findById(Long id);

    public Optional<MessagePatientUpdatedAt> findTopByOrderByPatientUpdatedAtDesc();

    public Iterable<MessagePatientIdAndVersionId> findByPatientUpdatedAtGreaterThanEqual(Instant instant);

    public long countByStatus(MessageStatus status);
}
