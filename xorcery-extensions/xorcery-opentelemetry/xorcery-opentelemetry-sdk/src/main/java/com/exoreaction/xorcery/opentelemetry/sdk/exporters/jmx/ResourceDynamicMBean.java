package com.exoreaction.xorcery.opentelemetry.sdk.exporters.jmx;

import io.opentelemetry.sdk.metrics.data.ExponentialHistogramPointData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;

import javax.management.*;
import javax.management.modelmbean.*;
import java.util.*;
import java.util.function.Consumer;

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

        switch (metric.getType())
        {
            case SUMMARY -> {
            }
            case HISTOGRAM -> {
                if (value instanceof HistogramPointData hpd)
                {
                    ensureMetricName(metric.getName()+".min", metric);
                    attributes.put(metric.getName()+".min", hpd.getMin());
                    attributes.put(metric.getName()+".max", hpd.getMax());
                    attributes.put(metric.getName()+".sum", hpd.getSum());
                    List<Double> boundaries = hpd.getBoundaries();
                    List<Long> counts = hpd.getCounts();
                    StringBuilder result = new StringBuilder();
                    for (int i = 0; i < boundaries.size(); i++) {
                        Double boundary = boundaries.get(i);
                        long count = counts.get(i);
                        if (count > 0)
                        {
                            if (!result.isEmpty())
                                result.append(", ");

                            result.append(boundary).append('=').append(count);
                        }
                    }
                    attributes.put(metric.getName()+".counts", result.toString());
                    attributes.put(metric.getName()+".count", hpd.getCount());
                }
            }
            case EXPONENTIAL_HISTOGRAM -> {
                if (value instanceof ExponentialHistogramPointData epd)
                {
                    ensureMetricName(metric.getName()+".min", metric);
                    attributes.put(metric.getName()+".min", epd.getMin());
                    attributes.put(metric.getName()+".max", epd.getMax());
                    attributes.put(metric.getName()+".sum", epd.getSum());
                    attributes.put(metric.getName()+".count", epd.getCount());
                }
            }
            default ->
            {
                ensureMetricName(metric.getName(), metric);
                attributes.put(metric.getName(), value);
            }
        }
    }

    private void ensureMetricName(String name, MetricData metric) throws MBeanException {
        if (mBeanInfo.getAttribute(name) == null)
        {
            List<MBeanAttributeInfo> newAttributes = new ArrayList<>();
            for (MBeanAttributeInfo attribute : mBeanInfo.getAttributes()) {
                newAttributes.add(attribute);
            }
            addAttributeInfos(metric, newAttributes::add);
            mBeanInfo = new ModelMBeanInfoSupport(
                    mBeanInfo.getClassName(),
                    mBeanInfo.getDescription(),
                    newAttributes.toArray(new ModelMBeanAttributeInfo[0]),
                    Arrays.asList(mBeanInfo.getConstructors()).toArray(new ModelMBeanConstructorInfo[0]),
                    Arrays.asList(mBeanInfo.getOperations()).toArray(new ModelMBeanOperationInfo[0]),
                    Arrays.asList(mBeanInfo.getNotifications()).toArray(new ModelMBeanNotificationInfo[0])
            );
        }
    }

    private void addAttributeInfos(MetricData metricData, Consumer<ModelMBeanAttributeInfo> consumer)
    {
        switch (metricData.getType()) {
            case LONG_GAUGE -> consumer.accept(new ModelMBeanAttributeInfo(metricData.getName(), Long.TYPE.getName(), metricData.getDescription(), true, false, false));
            case DOUBLE_GAUGE -> consumer.accept(new ModelMBeanAttributeInfo(metricData.getName(), Double.TYPE.getName(), metricData.getDescription(), true, false, false));
            case LONG_SUM -> consumer.accept(new ModelMBeanAttributeInfo(metricData.getName(), Long.TYPE.getName(), metricData.getDescription(), true, false, false));
            case DOUBLE_SUM -> consumer.accept(new ModelMBeanAttributeInfo(metricData.getName(), Double.TYPE.getName(), metricData.getDescription(), true, false, false));
            case SUMMARY -> String.class.getName();
            case HISTOGRAM ->
            {
                consumer.accept(new ModelMBeanAttributeInfo(metricData.getName()+".min", Double.TYPE.getName(), metricData.getDescription(), true, false, false));
                consumer.accept(new ModelMBeanAttributeInfo(metricData.getName()+".max", Double.TYPE.getName(), metricData.getDescription(), true, false, false));
                consumer.accept(new ModelMBeanAttributeInfo(metricData.getName()+".sum", Double.TYPE.getName(), metricData.getDescription(), true, false, false));
                consumer.accept(new ModelMBeanAttributeInfo(metricData.getName()+".counts", String.class.getName(), metricData.getDescription(), true, false, false));
                consumer.accept(new ModelMBeanAttributeInfo(metricData.getName()+".count", Long.TYPE.getName(), metricData.getDescription(), true, false, false));
            }
            case EXPONENTIAL_HISTOGRAM ->
            {
                consumer.accept(new ModelMBeanAttributeInfo(metricData.getName()+".min", Double.TYPE.getName(), metricData.getDescription(), true, false, false));
                consumer.accept(new ModelMBeanAttributeInfo(metricData.getName()+".max", Double.TYPE.getName(), metricData.getDescription(), true, false, false));
                consumer.accept(new ModelMBeanAttributeInfo(metricData.getName()+".sum", Double.TYPE.getName(), metricData.getDescription(), true, false, false));
                consumer.accept(new ModelMBeanAttributeInfo(metricData.getName()+".count", Long.TYPE.getName(), metricData.getDescription(), true, false, false));
            }
        };
    }

}
