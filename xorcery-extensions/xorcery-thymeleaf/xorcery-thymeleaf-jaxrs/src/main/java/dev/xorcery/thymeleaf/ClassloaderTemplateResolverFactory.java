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
package dev.xorcery.thymeleaf;

import dev.xorcery.configuration.Configuration;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

@Service(name="thymeleaf.classloader")
public class ClassloaderTemplateResolverFactory
    extends TemplateResolverFactory
{
    @Inject
    public ClassloaderTemplateResolverFactory(Configuration configuration) {
        super(new TemplateResolverConfiguration(configuration.getConfiguration("thymeleaf.classloader")), new ClassLoaderTemplateResolver());
    }

    @Override
    public ITemplateResolver provide() {
        return super.provide();
    }
}
