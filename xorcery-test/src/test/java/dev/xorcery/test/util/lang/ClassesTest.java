/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
