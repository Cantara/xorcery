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
package com.exoreaction.xorcery.handlebars.helpers;

import com.exoreaction.xorcery.jsonapi.Link;
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

    public Object stripquotes(String value, Options options)
    {
        if (value.startsWith("\"") && value.endsWith("\""))
            return value.substring(1, value.length()-1);
        else
            return value;
    }

    public Object isTemplate(Link link, Options options ) throws IOException
    {
        if (!new UriTemplate(link.getHref()).getTemplateVariables().isEmpty())
        {
            return options.fn();
        } else
        {
            return options.inverse();
        }
    }
}
