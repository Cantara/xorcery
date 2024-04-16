package com.exoreaction.xorcery.opentelemetry.sdk.exporters.jmx;

import io.opentelemetry.sdk.metrics.data.MetricData;

import javax.management.*;
import javax.management.modelmbean.*;
import java.util.*;

public class ResourceDynamicMBean
    implements DynamicMBean
{
    private volatile ModelMBeanInfoSupport mBeanInfo;
    private final Map<String, Object> attributes;

    public ResourceDynamicMBean(ModelMBeanInfoSupport mBeanInfo) {
        this.mBeanInfo = mBeanInfo;
        this.attributes = new HashMap<>();
    }

    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        Object value = attributes.get(attribute);
        if (value == null)
            throw new AttributeNotFoundException(attribute);
        return value;
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        // Ignore
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        try {
            AttributeList attributeList = new AttributeList(attributes.length);
            for (String attribute : attributes) {
                attributeList.add(new Attribute(attribute, getAttribute(attribute)));
            }
            return attributeList;
        } catch (Throwable e) {
            return new AttributeList();
        }
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        return null;
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
        return null;
    }

    @Override
    public ModelMBeanInfoSupport getMBeanInfo() {
        return mBeanInfo;
    }

    public Map<String, Object> getMetrics() {
        return attributes;
    }

    public void setMetric(MetricData metric, Object value) throws MBeanException {

        if (mBeanInfo.getAttribute(metric.getName()) == null)
        {
            List<MBeanAttributeInfo> newAttributes = new ArrayList<>();
            for (MBeanAttributeInfo attribute : mBeanInfo.getAttributes()) {
                newAttributes.add(attribute);
            }
            newAttributes.add(toAttributeInfo(metric));
            mBeanInfo = new ModelMBeanInfoSupport(
                    mBeanInfo.getClassName(),
                    mBeanInfo.getDescription(),
                    newAttributes.toArray(new ModelMBeanAttributeInfo[0]),
                    Arrays.asList(mBeanInfo.getConstructors()).toArray(new ModelMBeanConstructorInfo[0]),
                    Arrays.asList(mBeanInfo.getOperations()).toArray(new ModelMBeanOperationInfo[0]),
                    Arrays.asList(mBeanInfo.getNotifications()).toArray(new ModelMBeanNotificationInfo[0])
            );
        }
        attributes.put(metric.getName(), value);
    }

    private ModelMBeanAttributeInfo toAttributeInfo(MetricData metricData)
    {
        String type = switch (metricData.getType()) {
            case LONG_GAUGE -> Long.TYPE.getName();
            case DOUBLE_GAUGE -> Double.TYPE.getName();
            case LONG_SUM -> Long.TYPE.getName();
            case DOUBLE_SUM -> Double.TYPE.getName();
            case SUMMARY -> String.class.getName();
            case HISTOGRAM -> String.class.getName();
            case EXPONENTIAL_HISTOGRAM -> String.class.getName();
        };
        return new ModelMBeanAttributeInfo(metricData.getName(), type, metricData.getDescription(), true, false, false);
    }

}
