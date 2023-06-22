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
package com.exoreaction.xorcery.jetty.server.security.jwt;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.eclipse.jetty.security.UserPrincipal;

public class JwtUserPrincipal
    extends UserPrincipal
{
    public JwtUserPrincipal(String name, JwtCredential credential) {
        super(name, credential);
    }

    public DecodedJWT getJwt()
    {
        return ((JwtCredential)_credential).getJwt();
    }
}
