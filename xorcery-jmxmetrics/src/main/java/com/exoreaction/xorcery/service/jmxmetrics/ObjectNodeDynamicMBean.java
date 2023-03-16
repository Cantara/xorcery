package com.exoreaction.xorcery.service.jmxmetrics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.management.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class ObjectNodeDynamicMBean
        implements DynamicMBean {
    private final MBeanInfo mBeanInfo;
    private final Map<String, Function<JsonNode, Object>> attributeConverters = new HashMap<>();
    private final AtomicReference<ObjectNode> reference;

    public ObjectNodeDynamicMBean(MBeanInfo mBeanInfo, List<Function<JsonNode, Object>> converters, AtomicReference<ObjectNode> reference) {
        this.mBeanInfo = mBeanInfo;
        this.reference = reference;
        for (int i = 0; i < mBeanInfo.getAttributes().length; i++) {
            MBeanAttributeInfo attribute = mBeanInfo.getAttributes()[i];
            attributeConverters.put(attribute.getName(), converters.get(i));
        }
    }

    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        return attributeConverters.get(attribute).apply(reference.get().get(attribute));
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        // Ignore
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        AttributeList attributeList = new AttributeList();
        for (String attributeName : attributes) {
            try {
                attributeList.add(new Attribute(attributeName, getAttribute(attributeName)));
            } catch (AttributeNotFoundException | MBeanException | ReflectionException e) {
                throw new RuntimeException(e);
            }
        }
        return attributeList;
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        // No-op
        String[] names = new String[attributes.size()];
        for (int i = 0; i < attributes.size(); i++) {
            Attribute attribute = (javax.management.Attribute) attributes.get(i);
            names[i] = attribute.getName();
        }
        return getAttributes(names);
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
        return null;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return mBeanInfo;
    }
}
