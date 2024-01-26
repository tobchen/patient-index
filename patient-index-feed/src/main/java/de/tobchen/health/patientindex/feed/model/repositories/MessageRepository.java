package de.tobchen.health.patientindex.feed.model.repositories;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import de.tobchen.health.patientindex.feed.model.entities.MessageEntity;

public interface MessageRepository extends Repository<MessageEntity, Long>
{
    public Iterable<MessageEntity> saveAll(Iterable<MessageEntity> entities);

    public Optional<MessageEntity> findTopByOrderByPatientUpdatedAtDesc();
}
