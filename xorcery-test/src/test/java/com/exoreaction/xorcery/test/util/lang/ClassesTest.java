package com.exoreaction.xorcery.test.util.lang;

import com.exoreaction.xorcery.lang.Classes;
import com.exoreaction.xorcery.reactivestreams.api.MetadataJsonNode;
import com.exoreaction.xorcery.metadata.WithMetadata;
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
