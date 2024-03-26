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
package com.exoreaction.reactivestreams.server.extra.yaml.test;

import com.exoreaction.xorcery.reactivestreams.extras.publisher.YamlPublisher;
import com.exoreaction.xorcery.util.Resources;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

public class YamlPublisherTest {

    @Test
    public void testYamlPublisher() {

        YamlPublisher<Map<String, Object>> filePublisher = new YamlPublisher(Map.class, Resources.getResource("testevents.yaml").orElseThrow());

        List<Map<String, Object>> result = Flux
                .from(filePublisher)
                .toStream()
                .toList();

        System.out.println(result);

    }
}
