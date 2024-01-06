package de.tobchen.health.patientindex.model.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.springframework.lang.Nullable;

import de.tobchen.health.patientindex.model.enums.MessageStatus;

@Entity
@Table(indexes = {@Index(columnList = "status")})
public class PidFeedMsgEntity
{
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageStatus status;

    @Lob
    @Column(nullable = false)
    String message;

    @Lob
    @Nullable
    String response;

    // TODO Creation date (and indexing)

    protected PidFeedMsgEntity() { }

    public PidFeedMsgEntity(String message)
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

    public @Nullable String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
