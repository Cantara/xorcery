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
package dev.xorcery.function.test;

import dev.xorcery.function.LazyReference;
import org.junit.jupiter.api.Test;

import static dev.xorcery.function.LazyReference.lazyReference;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LazyReferenceTest {

    LazyReference<String> someValue = lazyReference();

    @Test
    public void testLazyReference()
    {
        assertEquals("This is an expensive value to calculate", getSomeValue());
        assertEquals("This is an expensive value to calculate", getSomeValue());
    }

    String getSomeValue(){
        return someValue.apply(()->"This is an expensive value to calculate");
    }

}