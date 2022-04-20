/*
 *  Copyright (C) 2018 Real Vision Group SEZC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.exoreaction.reactiveservices.json;

import jakarta.json.Json;
import jakarta.json.JsonStructure;
import jakarta.json.stream.JsonGenerator;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rickardoberg
 * @since 07/08/2017
 */
public class AbstractJsonElement
    implements JsonElement
{
    private final JsonStructure json;

    public AbstractJsonElement( JsonStructure json )
    {
        this.json = json;
    }

    @Override
    public JsonStructure json()
    {
        return json;
    }

    @Override
    public String toString()
    {
        StringWriter out = new StringWriter();
        Map<String,Object> config = new HashMap<>();
        config.put( JsonGenerator.PRETTY_PRINTING, Boolean.TRUE );
        Json.createWriterFactory( config ).createWriter( out ).write( json );
        return out.toString();
    }
}
