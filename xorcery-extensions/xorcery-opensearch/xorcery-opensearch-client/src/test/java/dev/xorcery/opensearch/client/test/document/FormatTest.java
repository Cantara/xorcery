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
package dev.xorcery.opensearch.client.test.document;

import dev.xorcery.util.Resources;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Optional;

@Disabled("random tests")
public class FormatTest {

    @Test
    public void formatTest()
    {
        System.out.println(String.format("numbers-%tF", System.currentTimeMillis()));
        System.out.println(String.format("numbers", System.currentTimeMillis()));
    }

    @Test
    public void resourceTest()
    {
        Optional<URL> resource = Resources.getResource("opensearch/templates/components/common.yaml");
        System.out.println(resource);
    }
}
