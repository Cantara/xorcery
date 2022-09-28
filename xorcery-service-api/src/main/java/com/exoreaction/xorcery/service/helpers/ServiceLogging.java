package com.exoreaction.xorcery.service.helpers;

import com.exoreaction.xorcery.server.model.ServiceIdentifier;
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

    default void connectedTo(ServiceIdentifier serviceIdentifier) {
        logger().info(marker(), "Connected to " + serviceIdentifier);
    }

    default void disconnectedFrom(ServiceIdentifier serviceIdentifier) {
        logger().info(marker(), "Disconnected from " + serviceIdentifier);
    }
}
