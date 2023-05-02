package com.exoreaction.xorcery.service.neo4j;

import java.util.Objects;

public record SemanticVersion(int major, int minor, int patch) {

    public static SemanticVersion from(String version) {
        Objects.requireNonNull(version);
        String[] parts = version.split("[.]");
        if (parts.length != 3) {
            throw new IllegalArgumentException("version must be compatible with \"<major>.<minor>.<patch>\"");
        }
        return new SemanticVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    public boolean isBreakingChange(SemanticVersion previous) {
        return major != previous.major || minor < previous.minor;
    }

    @Override
    public String toString() {
        return String.format("%d.%d.%d", major, minor, patch);
    }
}
