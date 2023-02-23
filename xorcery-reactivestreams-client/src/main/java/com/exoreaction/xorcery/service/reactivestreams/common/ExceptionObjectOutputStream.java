package com.exoreaction.xorcery.service.reactivestreams.common;

import java.io.*;

public class ExceptionObjectOutputStream
    extends ObjectOutputStream
{
    public ExceptionObjectOutputStream(OutputStream out) throws IOException {
        super(out);
        enableReplaceObject(true);
    }

    @Override
    protected Object replaceObject(Object obj) throws IOException {
        if (obj instanceof String) {
            return obj;
        } else if (obj.getClass().isArray()) {
            return obj;
        } else if (obj instanceof Enum) {
            return obj;
        } else if (obj instanceof Serializable) {
            return obj;
        } else {
            return null; // Replace unserializable objects with null;
        }

    }
}
