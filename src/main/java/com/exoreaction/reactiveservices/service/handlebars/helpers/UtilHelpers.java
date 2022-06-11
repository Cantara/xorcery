package com.exoreaction.reactiveservices.service.handlebars.helpers;

import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.TagType;

import java.io.IOException;
import java.util.Optional;

public class UtilHelpers {

    public CharSequence optional( Optional<?> value, Options options ) throws IOException
    {
        if ( value != null && value.isPresent() )
        {
            Object val = value.get();
            return options.fn( val );
        }
        else
        {
            if ( options.tagType == TagType.SECTION )
            {
                return options.inverse();
            }
            else
            {
                return null;
            }
        }
    }
}
