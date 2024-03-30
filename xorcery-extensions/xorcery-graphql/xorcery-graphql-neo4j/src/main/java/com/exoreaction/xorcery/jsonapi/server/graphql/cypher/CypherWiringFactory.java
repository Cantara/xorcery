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
package com.exoreaction.xorcery.jsonapi.server.graphql.cypher;

import com.exoreaction.xorcery.jsonapi.server.graphql.schema.TypeNameTypeResolver;
import graphql.schema.DataFetcher;
import graphql.schema.TypeResolver;
import graphql.schema.idl.FieldWiringEnvironment;
import graphql.schema.idl.InterfaceWiringEnvironment;
import graphql.schema.idl.NoopWiringFactory;
import graphql.schema.idl.UnionWiringEnvironment;

public class CypherWiringFactory
    extends NoopWiringFactory
{
    private final TypeResolver typenameTypeResolver = new TypeNameTypeResolver();
    private final DataFetcher cypherDataFetecher = new CypherDataFetcher();

    public CypherWiringFactory() {
    }

    @Override
    public boolean providesTypeResolver(InterfaceWiringEnvironment environment) {
        return true;
    }

    @Override
    public TypeResolver getTypeResolver(InterfaceWiringEnvironment environment) {
        return typenameTypeResolver;
    }

    @Override
    public boolean providesTypeResolver(UnionWiringEnvironment environment) {
        return true;
    }

    @Override
    public TypeResolver getTypeResolver(UnionWiringEnvironment environment) {
        return typenameTypeResolver;
    }

    @Override
    public boolean providesDataFetcher(FieldWiringEnvironment environment) {
        return environment.getParentType().getName().equals("Query");
    }

    @Override
    public DataFetcher getDataFetcher(FieldWiringEnvironment environment) {
        return cypherDataFetecher;
    }
}
