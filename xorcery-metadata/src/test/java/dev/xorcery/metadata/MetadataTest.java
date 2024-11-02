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
package dev.xorcery.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;

class MetadataTest {

    @Test
    public void testSerializeDeserialize() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        Metadata metadata = new Metadata.Builder()
                .add("foo", "bar")
                .build();

        String value = objectMapper.writeValueAsString(metadata);

        System.out.println(value);

        Metadata metadata2 = objectMapper.readValue(new StringReader(value), Metadata.class);

        System.out.println(metadata2);
    }

}