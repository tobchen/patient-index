package de.tobchen.health.patientindex.feed.model.entities;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PrePersist;

import org.springframework.lang.Nullable;

@Entity
public class MessageEntity
{
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, updatable = false)
    private Instant occuredAt;

    @Nullable
    private Instant sentAt;

    @Column(nullable = false, updatable = false)
    private String patientId;

    protected MessageEntity() { }

    public MessageEntity(String patientId, Instant occuredAt)
    {
        this.patientId = patientId;
        this.occuredAt = occuredAt;
    }

    @PrePersist
    private void onPrePersist()
    {
        occuredAt = Instant.now();
    } 

    public Long getId()
    {
        return id;
    }

    public Instant getOccuredAt()
    {
        return occuredAt;
    }

    public Instant getSentAt()
    {
        return sentAt;
    }

    public void setSentAt(Instant sentAt)
    {
        this.sentAt = sentAt;
    }

    public String getPatientId()
    {
        return patientId;
    }
}
