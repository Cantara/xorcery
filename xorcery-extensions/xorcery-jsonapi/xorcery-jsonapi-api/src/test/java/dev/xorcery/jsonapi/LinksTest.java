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
package dev.xorcery.jsonapi;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LinksTest {

    @Test
    public void getByRel()
    {
        // Given
        Links links = new Links.Builder()
                .link("somerel", "http://localhost/1")
                .link("thisrel somerelx", "http://localhost/2")
                .link("somerels", "http://localhost/3")
                .build();

        //When/Then
        Assertions.assertNull(links.getByRel("somexrel").map(Link::getHref).orElse(null));
        Assertions.assertEquals("http://localhost/1", links.getByRel("somerel").map(Link::getHref).orElse(null));
        Assertions.assertEquals("http://localhost/2", links.getByRel("thisrel somerelx").map(Link::getHref).orElse(null));
        Assertions.assertEquals("http://localhost/2", links.getByRel("somerelx thisrel").map(Link::getHref).orElse(null));
        Assertions.assertEquals("http://localhost/3", links.getByRel("somerels").map(Link::getHref).orElse(null));

    }
}
