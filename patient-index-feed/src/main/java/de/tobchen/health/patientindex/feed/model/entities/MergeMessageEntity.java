package de.tobchen.health.patientindex.feed.model.entities;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class MergeMessageEntity extends MessageEntity
{
    @Column(updatable = false)
    private String sourcePatientId;

    protected MergeMessageEntity() { }

    public MergeMessageEntity(String patientId, String sourcePatientId, Instant occuredAt)
    {
        super(patientId, occuredAt);

        this.sourcePatientId = sourcePatientId;
    }

    public String getSourcePatientId() {
        return sourcePatientId;
    }
}
