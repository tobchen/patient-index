package de.tobchen.health.patientindex.model.repositories;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import de.tobchen.health.patientindex.model.entities.PidFeedMsgEntity;

public interface PidFeedMsgRepository extends Repository<PidFeedMsgEntity, Long>
{
    public PidFeedMsgEntity save(PidFeedMsgEntity entity);

    public Optional<PidFeedMsgEntity> findById(Long id);
}
