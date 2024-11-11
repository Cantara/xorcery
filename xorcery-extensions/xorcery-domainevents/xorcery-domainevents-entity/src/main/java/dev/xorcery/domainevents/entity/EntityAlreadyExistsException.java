package dev.xorcery.domainevents.entity;

public class EntityAlreadyExistsException
    extends EntityException
{
    public EntityAlreadyExistsException(String entityType, String entityId) {
        super("Entity already exists", entityType, entityId);
    }
}
