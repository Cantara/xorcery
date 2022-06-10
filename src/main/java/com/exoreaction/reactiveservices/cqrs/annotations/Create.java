package com.exoreaction.reactiveservices.cqrs.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

/**
 * Used to annotate commands that create aggregates
 *
 * @author rickardoberg
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( value = {TYPE} )
public @interface Create
{
}
