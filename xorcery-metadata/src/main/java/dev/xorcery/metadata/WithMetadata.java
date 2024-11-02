package dev.xorcery.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;

public abstract class WithMetadata<T>
{
    private Metadata metadata;
    private T data;

    public WithMetadata() {
    }

    @JsonCreator(mode = PROPERTIES)
    public WithMetadata(@JsonProperty("metadata") Metadata metadata, @JsonProperty("data") T data) {
        this.metadata = metadata;
        this.data = data;
    }

    public void set(Metadata metadata, T data)
    {
        this.metadata = metadata;
        this.data = data;
    }

    public void set(WithMetadata<T> other)
    {
        this.metadata = other.metadata;
        this.data = other.data;
    }

    @JsonGetter
    public Metadata metadata()
    {
        return metadata;
    }

    @JsonGetter
    public T data()
    {
        return data;
    }
}
