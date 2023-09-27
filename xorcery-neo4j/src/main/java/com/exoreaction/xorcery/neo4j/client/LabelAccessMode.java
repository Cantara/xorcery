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
    private final long requiredLabelId;

    public static LoginContext getLoginContextForLabel(String labelName) {
        return new LoginContext(AuthSubject.AUTH_DISABLED, ClientConnectionInfo.EMBEDDED_CONNECTION) {
            public SecurityContext authorize(IdLookup idLookup, String dbName, AbstractSecurityLog securityLog) {
                return new SecurityContext(AuthSubject.AUTH_DISABLED, new LabelAccessMode(idLookup.getLabelId(labelName)), this.connectionInfo(), dbName);
            }
        };
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
    public boolean allowsTraverseAllNodesWithLabel(long label) {
        return label == requiredLabelId;
    }

    @Override
    public boolean disallowsTraverseLabel(long label) {
        return label != requiredLabelId;
    }

    @Override
    public boolean allowsTraverseNode(long... labels) {
        for (long label : labels) {
            if (label == requiredLabelId)
                return true;
        }
        return false;
    }
}
