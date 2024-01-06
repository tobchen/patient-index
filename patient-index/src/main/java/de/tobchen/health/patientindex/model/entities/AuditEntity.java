package de.tobchen.health.patientindex.model.entities;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import org.hl7.fhir.r5.model.AuditEvent.AuditEventAction;
import org.springframework.lang.Nullable;

@Entity
public class AuditEntity
{
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String categorySystem;
    @Column(nullable = false)
    private String categoryCode;

    @Column(nullable = false)
    private String codeSystem;
    @Column(nullable = false)
    private String codeCode;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditEventAction action;

    @Column(nullable = false)
    private Timestamp recordedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PatientEntity patient;

    @Column(nullable = false)
    private String sourceDisplay;
    @Nullable
    private String sourceAddress;

    @Column(nullable = false)
    private String destinationDisplay;
    @Nullable
    private String destinationAddress;

    @Lob
    private String query;

    protected AuditEntity() { }

    public AuditEntity(String categorySystem, String categoryCode, String codeSystem, String codeCode,
        AuditEventAction action, Timestamp recordedAt, PatientEntity patient, String sourceDisplay,
        @Nullable String sourceAddress, String destinationDisplay, @Nullable String destinationAddress,
        String query)
    {
        this.categorySystem = categorySystem;
        this.categoryCode = categoryCode;

        this.codeSystem = codeSystem;
        this.codeCode = codeCode;

        this.action = action;

        this.recordedAt = recordedAt;

        this.patient = patient;

        this.sourceDisplay = sourceDisplay;
        this.sourceAddress = sourceAddress;

        this.destinationDisplay = destinationDisplay;
        this.destinationAddress = destinationAddress;

        this.query = query;
    }

    public Long getId() {
        return id;
    }

    public String getCategorySystem() {
        return categorySystem;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getCodeSystem() {
        return codeSystem;
    }

    public String getCodeCode() {
        return codeCode;
    }

    public AuditEventAction getAction() {
        return action;
    }

    public Timestamp getRecordedAt() {
        return recordedAt;
    }

    public PatientEntity getPatient() {
        return patient;
    }

    public String getSourceDisplay() {
        return sourceDisplay;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public String getDestinationDisplay() {
        return destinationDisplay;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public String getQuery() {
        return query;
    }
}
