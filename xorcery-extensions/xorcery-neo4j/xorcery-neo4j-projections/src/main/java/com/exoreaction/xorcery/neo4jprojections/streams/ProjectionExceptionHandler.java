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
package com.exoreaction.xorcery.neo4jprojections.streams;

import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
import com.lmax.disruptor.ExceptionHandler;
import org.apache.logging.log4j.LogManager;
import org.reactivestreams.Subscription;

public class ProjectionExceptionHandler implements ExceptionHandler<MetadataEvents> {
    private final Subscription subscription;

    public ProjectionExceptionHandler(Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public void handleEventException(Throwable ex, long sequence, MetadataEvents event) {
        LogManager.getLogger(getClass()).error("Cancelled subscription", ex);
        try {
            subscription.cancel();
        } catch (Exception e) {
            LogManager.getLogger(getClass()).error("Failed to cancel subscription", ex);
        }
        throw new RuntimeException("Projection cancelled", ex);
    }

    @Override
    public void handleOnStartException(Throwable ex) {
        LogManager.getLogger(getClass()).warn("Cancelled subscription", ex);
        try {
            subscription.cancel();
        } catch (Exception e) {
            LogManager.getLogger(getClass()).error("Failed to cancel subscription", ex);
        }
        throw new RuntimeException("Projection cancelled", ex);
    }

    @Override
    public void handleOnShutdownException(Throwable ex) {
        LogManager.getLogger(getClass()).warn("Cancelled subscription", ex);
        try {
            subscription.cancel();
        } catch (Exception e) {
            LogManager.getLogger(getClass()).error("Failed to cancel subscription", ex);
        }
    }
}
