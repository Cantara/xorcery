package com.exoreaction.xorcery.jsonschema.server.annotations;

import com.exoreaction.xorcery.jsonschema.model.Types;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;

/**
 * @author rickardoberg
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( value = {FIELD, PARAMETER} )
public @interface AttributeSchema
{
    String title();

    String description();

    boolean required() default false;

    Types type() default Types.String;

    // Optional, only if type is String
    Class<? extends Enum> values() default Enum.class;
}
