package dev.xorcery.test.util.lang;

import dev.xorcery.lang.Classes;
import dev.xorcery.metadata.WithMetadata;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.Arrays;

public class ClassesTest {

    @Test
    public void resolveActualTypeArgs()
    {
        Type type = Classes.typeOrBound(Classes.resolveActualTypeArgs(MetadataJsonNode.class, WithMetadata.class)[0]);
        System.out.println(Arrays.asList(type));
    }
}
