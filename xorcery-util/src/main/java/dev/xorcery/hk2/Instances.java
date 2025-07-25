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
package dev.xorcery.hk2;

import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InstantiationService;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public interface Instances {

    /**
     * Helper method for Factory implementations to extract the @Instance name from an injectee point.
     * This makes it possible to create named instances, which is useful in particular for creating
     * clients based on different named configurations.
     *
     * @param instantiationService
     * @return
     */
    static String name(InstantiationService instantiationService) {
        Injectee parentInjectee = instantiationService.getInstantiationData().getParentInjectee();
        if (parentInjectee == null)
            return null;
        if (parentInjectee.getParent() instanceof Constructor<?> constructor) {
            for (Annotation annotation : constructor.getParameterAnnotations()[parentInjectee.getPosition()]) {
                if (annotation instanceof Instance instance)
                    return instance.value();
            }
        } else if (parentInjectee.getParent() instanceof Method method) {
            for (Annotation annotation : method.getParameterAnnotations()[parentInjectee.getPosition()]) {
                if (annotation instanceof Instance instance)
                    return instance.value();
            }
        } else if (parentInjectee.getParent() instanceof Field field) {
            for (Annotation annotation : field.getAnnotations()) {
                if (annotation instanceof Instance instance)
                    return instance.value();
            }
        } else if (parentInjectee.getParent() instanceof Parameter parameter) {
            for (Annotation annotation : parameter.getAnnotations()) {
                if (annotation instanceof Instance instance)
                    return instance.value();
            }
        }
        return null;
    }
}
