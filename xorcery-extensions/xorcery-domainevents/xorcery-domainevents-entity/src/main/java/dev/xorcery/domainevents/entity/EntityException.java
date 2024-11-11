package dev.xorcery.domainevents.entity;

public class EntityException
    extends Exception
{
    private final String entityType;
    private final String entityId;

    public EntityException(String message, String entityType, String entityId) {
        super(message);
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public EntityException(Throwable cause, String entityType, String entityId) {
        super(cause);
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }
}
