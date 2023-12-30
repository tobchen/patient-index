package de.tobchen.health.patientindex.events;

import org.springframework.context.ApplicationEvent;

public class PatientEvent extends ApplicationEvent
{
    private final Type type;
    private final String id;
    private final String otherId;

    public PatientEvent(Object source, Type type, String id, String otherId)
    {
        super(source);

        this.type = type;
        this.id = id;
        this.otherId = otherId;
    }

    public Type getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getOtherId() {
        return otherId;
    }

    public enum Type {
        CREATE,
        UPDATE,
        MERGE,
    }
}
