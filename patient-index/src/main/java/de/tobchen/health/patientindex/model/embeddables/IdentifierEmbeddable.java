package de.tobchen.health.patientindex.model.embeddables;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

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
    public boolean equals(Object obj) {
        boolean result;

        if (obj != null && obj instanceof IdentifierEmbeddable)
        {
            var other = (IdentifierEmbeddable) obj;
            result = Objects.equals(system, other.system) && Objects.equals(val, other.val);
        }
        else
        {
            result = super.equals(obj);
        }

        return result;
    }
}
