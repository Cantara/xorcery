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
package com.exoreaction.xorcery.neo4j.client;

import org.neo4j.internal.kernel.api.connectioninfo.ClientConnectionInfo;
import org.neo4j.internal.kernel.api.security.AbstractSecurityLog;
import org.neo4j.internal.kernel.api.security.AuthSubject;
import org.neo4j.internal.kernel.api.security.LoginContext;
import org.neo4j.internal.kernel.api.security.SecurityContext;
import org.neo4j.kernel.impl.api.security.OverriddenAccessMode;

/**
 * AccessMode that overrides FULL
 */
public class LabelAccessMode
        extends OverriddenAccessMode {
    private final int requiredLabelId;

    public static LoginContext getLoginContextForLabel(String labelName) {
        return new LabelLoginContext(labelName);
    }

    public LabelAccessMode(int labelId) {
        super(Static.FULL, Static.FULL);
        this.requiredLabelId = labelId;
    }

    @Override
    public boolean allowsTraverseAllLabels() {
        return false;
    }

    @Override
    public boolean allowsTraverseAllNodesWithLabel(int label) {
        return label == requiredLabelId;
    }

    @Override
    public boolean disallowsTraverseLabel(int label) {
        return label != requiredLabelId;
    }

    @Override
    public boolean allowsTraverseNode(int... labels) {
        for (long label : labels) {
            if (label == requiredLabelId)
                return true;
        }
        return false;
    }

    private static class LabelLoginContext extends LoginContext {
        private final String labelName;

        public LabelLoginContext(String labelName) {
            super(AuthSubject.AUTH_DISABLED, ClientConnectionInfo.EMBEDDED_CONNECTION);
            this.labelName = labelName;
        }

        public SecurityContext authorize(IdLookup idLookup, String dbName, AbstractSecurityLog securityLog) {
            return new SecurityContext(AuthSubject.AUTH_DISABLED, new LabelAccessMode(idLookup.getLabelId(labelName)), this.connectionInfo(), dbName);
        }
    }
}
