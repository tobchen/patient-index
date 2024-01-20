package de.tobchen.health.patientindex.model.embeddables;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.springframework.lang.Nullable;

@Embeddable
public class IdentifierEmbeddable
{
    @Column(nullable = false)
    private String system;
    @Column(nullable = false)
    private String val;

    protected IdentifierEmbeddable() { }

    public IdentifierEmbeddable(String system, String val)
    {
        this.system = system;
        this.val = val;
    }

    public String getSystem() {
        return system;
    }

    public String getVal() {
        return val;
    }

    @Override
    public int hashCode() {
        return Objects.hash(system, val);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        boolean result;

        if (obj instanceof IdentifierEmbeddable identifier)
        {
            result = Objects.equals(system, identifier.system) && Objects.equals(val, identifier.val);
        }
        else
        {
            result = super.equals(obj);
        }

        return result;
    }
}
