package dev.xorcery.hk2;

import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InstantiationService;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public interface Instances {

    /**
     * Helper method for Factory implementations to extract the @Instance name from an injectee point.
     * This makes it possible to create named instances, which is useful in particular for creating
     * clients based on different named configurations.
     * @param instantiationService
     * @return
     */
    static String name(InstantiationService instantiationService)
    {
        Injectee parentInjectee = instantiationService.getInstantiationData().getParentInjectee();
        if (parentInjectee.getParent() instanceof Constructor<?> constructor)
        {
            for (Annotation annotation : constructor.getParameterAnnotations()[parentInjectee.getPosition()]) {
                if (annotation instanceof Instance instance)
                    return instance.value();
            }
        } else if (parentInjectee.getParent() instanceof Method method)
        {
            for (Annotation annotation : method.getParameterAnnotations()[parentInjectee.getPosition()]) {
                if (annotation instanceof Instance instance)
                    return instance.value();
            }
        } else if (parentInjectee.getParent() instanceof Field field)
        {
            for (Annotation annotation : field.getAnnotations()) {
                if (annotation instanceof Instance instance)
                    return instance.value();
            }
        }
        return null;
    }
}
