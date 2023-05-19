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
package com.exoreaction.xorcery.service.handlebars;


import com.exoreaction.xorcery.service.handlebars.helpers.UtilHelpers;
import com.github.jknack.handlebars.Handlebars;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service
public class HandlebarsFactory
        implements Factory<Handlebars> {

    private final Handlebars handlebars;

    @Inject
    public HandlebarsFactory() {
        handlebars = new Handlebars()
                .registerHelpers(new UtilHelpers());

        handlebars.with(
                new SystemClassLoaderTemplateLoader("META-INF/handlebars/templates/", ".html")
        );

        handlebars.getCache().setReload(true);
    }

    @Override
    @Singleton
    public Handlebars provide() {
        return handlebars;
    }

    @Override
    public void dispose(Handlebars instance) {
    }
}
