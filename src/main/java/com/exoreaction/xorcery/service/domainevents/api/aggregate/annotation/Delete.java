package com.exoreaction.xorcery.service.domainevents.api.aggregate.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

/**
 * Used to annotate commands that delete aggregates/entities
 *
 * @author rickardoberg
\ */
@Retention( RetentionPolicy.RUNTIME )
@Target( value = {TYPE} )
public @interface Delete
{
}
