package dev.xorcery.domainevents.entity;

public class EntityWrongVersionException
    extends EntityException
{
    public EntityWrongVersionException(String expectedVersion, String actualVersion, String entityType, String entityId) {
        super(String.format("Wrong entity version(expected=%s,actual=%s", expectedVersion, actualVersion), entityType, entityId);
    }
}
