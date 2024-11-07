package dev.xorcery.domainevents.context;

public class EntityNotFoundException
    extends Exception
{
    public EntityNotFoundException() {
        super("Entity not found");
    }
}
