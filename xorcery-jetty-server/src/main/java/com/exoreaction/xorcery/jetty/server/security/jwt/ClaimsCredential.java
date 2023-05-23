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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import org.eclipse.jetty.util.security.Credential;

public class ClaimsCredential
    extends Credential
{
    private Claims claims;

    public ClaimsCredential(Claims claims) {
        this.claims = claims;
    }

    @Override
    public boolean check(Object credentials) {
        return true;
    }

    public Claims getClaims() {
        return claims;
    }

    @Override
    public String toString() {
        return Jwts.builder().setClaims(claims).compact();
    }
}
