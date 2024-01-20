package de.tobchen.health.patientindex.feed.model.repositories;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import de.tobchen.health.patientindex.feed.model.entities.MessageEntity;
import de.tobchen.health.patientindex.feed.model.projections.MessageOccuredAt;

public interface MessageRepository extends Repository<MessageEntity, Long>
{
    public Optional<MessageOccuredAt> findTopByOrderByOccuredAtDesc();    
}
