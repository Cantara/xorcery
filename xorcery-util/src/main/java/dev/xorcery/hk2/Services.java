package dev.xorcery.hk2;

import jakarta.annotation.Priority;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.internal.StarFilter;

import java.lang.annotation.Annotation;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import static dev.xorcery.lang.Classes.getAllTypes;

public interface Services {

    /**
     * Use this when injecting a parent type of the instance you are looking for.
     * <p>
     * Example
     * IterableProvider<Handler> handlers = ...
     * ofType(handlers, ServletContextHandler.class).ifPresent(sch -> {...});
     *
     * @param instances
     * @param instanceType
     * @param <T>
     * @return
     */
    static <T> Optional<T> ofType(IterableProvider<?> instances, Class<T> instanceType) {
        for (Object instance : instances) {
            if (instanceType.isInstance(instance))
                return Optional.of(instanceType.cast(instance));
        }
        return Optional.empty();
/*
        Iterator<Object> typeInstances = instances.ofType(instanceType).iterator();
        if (typeInstances.hasNext())
            return Optional.of(instanceType.cast(typeInstances.next()));
        else
            return Optional.empty();
*/
    }

    static <T> Optional<T> ofType(ServiceLocator serviceLocator, Class<T> instanceType, Annotation... qualifiers) {
        return getAllTypes(instanceType)
                .mapMulti((type, mapper) -> serviceLocator.getAllServices(type, qualifiers).forEach(mapper))
                .filter(instanceType::isInstance)
                .map(instanceType::cast)
                .findFirst();
    }

    static <T> Stream<ServiceHandle<T>> allOfTypeRanked(ServiceLocator serviceLocator, Class<T> instanceType)
    {
        return serviceLocator.getAllServiceHandles(StarFilter.getDescriptorFilter())
                .stream()
                .filter(handle -> handle.getActiveDescriptor().getAdvertisedContracts().stream().anyMatch(className ->
                {
                    try {
                        return instanceType.isAssignableFrom(Class.forName(className));
                    } catch (Throwable e)
                    {
                        return false;
                    }
                }))
                .peek(sh ->
                {
                    try {
                        Class<?> implClass = Class.forName(sh.getActiveDescriptor().getImplementation());
                        sh.getActiveDescriptor().setRanking(Optional.ofNullable(implClass.getAnnotation(Priority.class))
                                .map(Priority::value)
                                .orElse(0));
                    } catch (ClassNotFoundException e) {
                        // Ignore
                    }
                })
                .sorted(Comparator.comparingInt(sh -> sh.getActiveDescriptor().getRanking()))
                .map(h -> (ServiceHandle<T>)h);
    }
}
