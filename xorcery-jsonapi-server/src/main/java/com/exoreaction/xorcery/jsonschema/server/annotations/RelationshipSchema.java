package com.exoreaction.xorcery.jsonschema.server.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

/**
 * @author rickardoberg
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( value = {FIELD} )
public @interface RelationshipSchema
{
    String title();

    String description();

    Cardinality cardinality() default Cardinality.one;
}
