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
package com.exoreaction.xorcery.thymeleaf;

import com.exoreaction.xorcery.configuration.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ITemplateResolver;

@Service
public class TemplateEngineFactory
        implements Factory<ITemplateEngine> {

    private final TemplateEngine templateEngine;

    @Inject
    public TemplateEngineFactory(
            Iterable<ITemplateResolver> resolvers,
            Configuration configuration) {
        templateEngine = new TemplateEngine();
        for (ITemplateResolver resolver : resolvers) {
            templateEngine.addTemplateResolver(resolver);
        }
    }

    @Override
    @Singleton
    public ITemplateEngine provide() {
        return templateEngine;
    }

    @Override
    public void dispose(ITemplateEngine instance) {

    }
}
