package de.tobchen.health.patientindex.feed.model.entities;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.springframework.lang.Nullable;

import de.tobchen.health.patientindex.feed.model.enums.MessageStatus;

@Entity
@Table(indexes = { @Index(columnList = "patientUpdatedAt"), @Index(columnList = "status") })
public class MessageEntity
{
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, updatable = false)
    private String patientId;

    @Column(nullable = false, updatable = false)
    private String patientVersionId;

    @Column(nullable = false, updatable = false)
    private Instant patientUpdatedAt;

    @Nullable
    @Column(updatable = false)
    private String linkedPatientId;

    @Column(nullable = false, updatable = false)
    private Instant recordedAt;

    @Column(nullable = false)
    private MessageStatus status;

    protected MessageEntity() { }

    public MessageEntity(String patientId, String patientVersionId, Instant patientUpdatedAt,
        @Nullable String linkedPatientId)
    {
        this.patientId = patientId;
        this.patientVersionId = patientVersionId;
        this.patientUpdatedAt = patientUpdatedAt;
        this.linkedPatientId = linkedPatientId;
        this.recordedAt = Instant.now();
        this.status = MessageStatus.QUEUED;
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

    public String getPatientVersionId() {
        return patientVersionId;
    }

    public Instant getPatientUpdatedAt()
    {
        return patientUpdatedAt;
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

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }
}
