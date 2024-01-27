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
    private String patientId;

    @Nullable
    @Column(updatable = false)
    private String patientVersionId;

    @Column(nullable = false, updatable = false)
    private Instant patientUpdatedAt;

    @Nullable
    @Column(updatable = false)
    private String linkedPatientId;

    @Column(nullable = false, updatable = false)
    private Instant recordedAt;

    @Nullable
    private Instant sentAt;

    protected MessageEntity() { }

    public MessageEntity(String patientId, String patientVersionId, Instant patientUpdatedAt,
        String otherPatientId)
    {
        this.patientId = patientId;
        this.patientVersionId = patientVersionId;
        this.patientUpdatedAt = patientUpdatedAt;
        this.linkedPatientId = otherPatientId;
        this.recordedAt = Instant.now();
    }

    @PrePersist
    private void onPrePersist()
    {
        patientUpdatedAt = Instant.now();
    } 

    public Long getId()
    {
        return id;
    }

    public @Nullable String getPatientVersionId() {
        return patientVersionId;
    }

    public Instant getPatientUpdatedAt()
    {
        return patientUpdatedAt;
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

    public @Nullable String getLinkedPatientId() {
        return linkedPatientId;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
