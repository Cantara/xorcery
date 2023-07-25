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
package com.exoreaction.xorcery.jsonapi;

import com.exoreaction.xorcery.jsonapi.ResourceObjectIdentifier;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

class ResourceObjectIdentifierTest {

    @Test
    void testEquals() {
        ResourceObjectIdentifier roi1 = new ResourceObjectIdentifier.Builder("type1", "id1").build();
        ResourceObjectIdentifier roi12 = new ResourceObjectIdentifier.Builder("type1", "id1").build();
        ResourceObjectIdentifier roi2 = new ResourceObjectIdentifier.Builder("type2", "id2").build();

        assertThat(roi1.equals(roi2), equalTo(false));
        assertThat(roi1.equals(roi12), equalTo(true));
    }
}