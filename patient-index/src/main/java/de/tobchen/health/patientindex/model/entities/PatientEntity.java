package de.tobchen.health.patientindex.model.entities;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.lang.Nullable;

import de.tobchen.health.patientindex.model.embeddables.IdentifierEmbeddable;

@Entity
@Table(indexes = {@Index(columnList = "resourceId")})
public class PatientEntity
{
    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String resourceId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(indexes = {@Index(columnList = "system"), @Index(columnList = "val")})
    private Set<IdentifierEmbeddable> identifiers = new HashSet<>();

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY)
    private PatientEntity mergedInto;

    private Instant updatedAt;

    protected PatientEntity() { }

    public PatientEntity(String resourceId)
    {
        this.resourceId = resourceId;
        this.updatedAt = Instant.now();
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

    public void setUpdatedAt() {
        setUpdatedAt(Instant.now());
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
