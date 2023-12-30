package de.tobchen.health.patientindex.ihe.model.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;

import de.tobchen.health.patientindex.ihe.model.enums.MessageStatus;

@Entity
@Table(indexes = {@Index(columnList = "status")})
public class PatientIdentityFeedMessageEntity
{
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private MessageStatus status;

    @Lob
    @Column(nullable = false)
    String message;

    @Lob
    String response;

    // TODO Creation date (and indexing)

    protected PatientIdentityFeedMessageEntity() { }

    public PatientIdentityFeedMessageEntity(String message)
    {
        this.message = message;
        
        this.status = MessageStatus.QUEUED;
    }

    public Long getId() {
        return id;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
