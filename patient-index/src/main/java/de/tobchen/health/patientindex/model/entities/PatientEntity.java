package de.tobchen.health.patientindex.model.entities;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.springframework.lang.Nullable;

import de.tobchen.health.patientindex.model.embeddables.IdentifierEmbeddable;

@Entity
@Table(indexes = {@Index(columnList = "resourceId"), @Index(columnList = "updatedAt")})
public class PatientEntity
{
    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String resourceId;

    @Column(nullable = false)
    private Long versionId;

    @Column(nullable = false)
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(indexes = {@Index(columnList = "system"), @Index(columnList = "val")})
    private Set<IdentifierEmbeddable> identifiers = new HashSet<>();

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    private PatientEntity mergedInto;

    protected PatientEntity() { }

    public PatientEntity(String resourceId)
    {
        this.resourceId = resourceId;
        this.versionId = Long.valueOf(0);
        this.updatedAt = Instant.now();
    }

    @PrePersist
    @PreUpdate
    public void prePersistOrUpdate()
    {
        this.updatedAt = Instant.now();
    }

    public void incrementVersion()
    {
        this.versionId = Long.valueOf(this.versionId.longValue() + 1);
    }

    public Long getId() {
        return id;
    }

    public String getResourceId() {
        return resourceId;
    }

    public Set<IdentifierEmbeddable> getIdentifiers() {
        return identifiers;
    }

    public @Nullable PatientEntity getMergedInto() {
        return mergedInto;
    }

    public void setMergedInto(PatientEntity mergedInto) {
        this.mergedInto = mergedInto;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersionId() {
        return versionId;
    }
}
