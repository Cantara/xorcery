package dev.xorcery.hk2;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Put this on fields injected by Factory calls, so that the Factory can create an instance with the given name. Typically
 * each name will be associated with different configuration settings.
 */
@Documented
@Retention(RUNTIME)
public @interface Instance {

    /** The name.
     * @return the name.
     */
    String value() default "";
}
