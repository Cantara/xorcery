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
package dev.xorcery.neo4j.client;

import org.neo4j.internal.kernel.api.LabelsSupplier;
import org.neo4j.internal.kernel.api.connectioninfo.ClientConnectionInfo;
import org.neo4j.internal.kernel.api.security.*;
import org.neo4j.kernel.database.PrivilegeDatabaseReference;
import org.neo4j.kernel.impl.api.security.OverriddenAccessMode;
import org.neo4j.storageengine.api.PropertySelection;

import java.util.function.IntPredicate;
import java.util.function.Supplier;

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
        super(StaticAccessMode.FULL, StaticAccessMode.FULL);
        this.requiredLabelId = labelId;
    }

    @Override
    public boolean allowsTraverseNode(LabelsSupplier labels, SelectedPropertiesProvider selectedPropertiesProvider) {
        return labels.getLabels().contains(requiredLabelId);
    }

    @Override
    public boolean allowsReadNodeProperties(LabelsSupplier labels, int[] propertyKeys, Supplier<SelectedPropertiesProvider> propertyProvider) {
        return labels.getLabels().contains(requiredLabelId);
    }

    @Override
    public boolean allowsTraverseAndReadAllMatchingNodeProperties(int[] labels, int[] propertyKeys) {
        for (int label : labels) {
            if (label == requiredLabelId)
                return super.allowsTraverseAndReadAllMatchingNodeProperties(labels, propertyKeys);
        }
        return false;
    }

    @Override
    public IntPredicate allowedToReadNodeProperties(LabelsSupplier labels, Supplier<SelectedPropertiesProvider> propertyProvider, PropertySelection selection) {
        boolean allowed = labels.getLabels().contains(requiredLabelId);
        return v -> allowed;
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

        @Override
        public SecurityContext authorize(IdLookup idLookup, PrivilegeDatabaseReference privilegeDatabaseReference, AbstractSecurityLog abstractSecurityLog) {
            return new SecurityContext(AuthSubject.AUTH_DISABLED, new LabelAccessMode(idLookup.getLabelId(labelName)), this.connectionInfo(), privilegeDatabaseReference.name());
        }
    }
}
