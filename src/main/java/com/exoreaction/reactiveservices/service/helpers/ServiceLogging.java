package com.exoreaction.reactiveservices.service.helpers;

import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceReference;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;

public interface ServiceLogging {
    Logger logger();

    Marker marker();

    default void starting() {
        logger().debug(marker(), "Starting");
    }

    default void started() {
        logger().debug(marker(), "Started");
    }

    default void stopping() {
        logger().debug(marker(), "Stopping");
    }

    default void stopped() {
        logger().debug(marker(), "Stopped");
    }

    default void connectedTo(ServiceReference serviceReference) {
        logger().info(marker(), "Connected to " + serviceReference);
    }

    default void disconnectedFrom(ServiceReference serviceReference) {
        logger().info(marker(), "Disconnected from " + serviceReference);
    }
}
