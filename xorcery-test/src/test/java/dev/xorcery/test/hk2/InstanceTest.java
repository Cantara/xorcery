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
package dev.xorcery.test.hk2;

import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.core.Xorcery;
import dev.xorcery.hk2.Instance;
import dev.xorcery.hk2.Instances;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.InstantiationService;
import org.glassfish.hk2.api.PerLookup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.jvnet.hk2.annotations.Service;

public class InstanceTest {

    private Thing thing;
    public @Inject @Instance("othername") Thing thing2;

    @Test
    public void testInstance() throws Exception {

        try(Xorcery xorcery = new Xorcery(new ConfigurationBuilder().addTestDefaults().build())){

            xorcery.getServiceLocator().inject(this);

            Assertions.assertEquals("somename", thing.name());
            Assertions.assertEquals("othername", thing2.name());
        }
    }

    @Inject
    public void bind(@Instance("somename") Thing thing){
        this.thing = thing;
    }

    public record Thing(String name){
    }

    @Service
    public static class ThingFactory
    implements Factory<Thing>{

        private final InstantiationService instantiationService;

        @Inject
        public ThingFactory(InstantiationService instantiationService) {
            this.instantiationService = instantiationService;
        }

        @Override
        @PerLookup
        public Thing provide() {
            String name = Instances.name(instantiationService);
            return new Thing(name);
        }

        @Override
        public void dispose(Thing instance) {
        }
    }
}
