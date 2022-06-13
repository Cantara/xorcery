package com.exoreaction.reactiveservices.service.handlebars.helpers;

import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.TagType;
import com.github.jknack.handlebars.helper.EachHelper;
import org.glassfish.jersey.uri.UriTemplate;

import java.io.IOException;
import java.util.Optional;

public class UtilHelpers {

    public Object optional( Optional<?> value, Options options ) throws IOException
    {
        if ( value != null && value.isPresent() )
        {
            Object val = value.get();
            if ( options.tagType == TagType.SECTION )
            {
                return options.fn(val);
            }
            else
            {
                return val;
            }
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

    public Object parameters(Link link, Options options ) throws IOException
    {
        return new EachHelper().apply( new UriTemplate( link.getHref() ).getTemplateVariables(), options );
    }
}
