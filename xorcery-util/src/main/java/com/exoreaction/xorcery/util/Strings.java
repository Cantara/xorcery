package com.exoreaction.xorcery.util;

public final class Strings {
    public static String capitalize(String name) {
        if (name == null || name.length() == 0)
            return name;
        int offset1 = name.offsetByCodePoints(0, 1);
        return name.substring(0, offset1).toUpperCase() +
                name.substring(offset1);
    }
}
