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
package dev.xorcery.reactivestreams.disruptor;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.ProducerType;
import dev.xorcery.configuration.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public record DisruptorConfiguration(Configuration configuration) {
    public int getSize() {
        return configuration.getInteger("size").orElse(256);
    }

    public String getPrefix() {
        return configuration.getString("prefix").orElse("DisruptorFlux-");
    }

    public ProducerType getProducerType() {
        return configuration.getEnum("producerType", ProducerType.class).orElse(ProducerType.MULTI);
    }

    public WaitStrategy getWaitStrategy() {

        return configuration.getEnum("waitStrategy", WaitStrategyTypes.class).map(type ->
                switch (type) {
                    case blocking -> new BlockingWaitStrategy();
                    case busyspin -> new BusySpinWaitStrategy();
                    case liteblocking -> new LiteBlockingWaitStrategy();
                    case litetimeoutblocking ->
                            new LiteTimeoutBlockingWaitStrategy(getTimeout().toMillis(), TimeUnit.MILLISECONDS);
                    case phasedbackoff ->
                            new PhasedBackoffWaitStrategy(getSpinTimeout().toMillis(), getYieldTimeout().toMillis(), TimeUnit.MILLISECONDS, new BlockingWaitStrategy());
                    case sleeping -> new SleepingWaitStrategy();
                    case timeoutblocking ->
                            new TimeoutBlockingWaitStrategy(getTimeout().toMillis(), TimeUnit.MILLISECONDS);
                    case yielding -> new YieldingWaitStrategy();
                }
        ).orElse(new BlockingWaitStrategy());
    }

    private Duration getTimeout() {
        return Duration.parse("PT" + configuration.getString("timeout").orElse("1s"));
    }

    private Duration getSpinTimeout() {
        return Duration.parse("PT" + configuration.getString("spinTimeout").orElse("1s"));
    }

    private Duration getYieldTimeout() {
        return Duration.parse("PT" + configuration.getString("yieldTimeout").orElse("1s"));
    }

    public Duration getShutdownTimeout() {
        return Duration.parse("PT" + configuration.getString("shutdownTimeout").orElse("60s"));
    }

    enum WaitStrategyTypes {
        blocking,
        busyspin,
        liteblocking,
        litetimeoutblocking,
        phasedbackoff,
        sleeping,
        timeoutblocking,
        yielding
    }
}
