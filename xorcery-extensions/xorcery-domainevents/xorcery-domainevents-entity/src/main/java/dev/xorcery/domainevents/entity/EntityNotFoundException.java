package dev.xorcery.domainevents.entity;

public class EntityNotFoundException
    extends EntityException
{
    public EntityNotFoundException(String entityType, String entityId) {
        super("Entity not found", entityType, entityId);
    }
}
