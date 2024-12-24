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
package dev.xorcery.opentelemetry.sdk;


import dev.xorcery.configuration.Configuration;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.util.List;

/**
 * Uses declarative rules to decide sampling result.
 * <p>
 * All spans are included by default. Exclude rules then modifies this. Then if a span is excluded by a rule an include rule can override it.
 * <p>
 * Example to only include API request log:
 * excludes:
 *   - name: GET
 * includes:
 *   - name: GET /api
 *
 */
@Service
@ContractsProvided({Sampler.class})
public class RuleBasedSampler
        implements Sampler {
    private final List<SampleRule> excludeRules;
    private final List<SampleRule> includeRules;

    @Inject
    public RuleBasedSampler(Configuration configuration) {

        RuleBasedSamplerConfiguration ruleConfig = RuleBasedSamplerConfiguration.get(configuration);
        excludeRules = ruleConfig.getExcludes();
        includeRules = ruleConfig.getIncludes();
    }

    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind, Attributes attributes, List<LinkData> parentLinks) {
        for (SampleRule excludeRule : excludeRules) {
            if (excludeRule.matches(name, spanKind, attributes, parentLinks)) {
                for (SampleRule includeRule : includeRules) {
                    if (includeRule.matches(name, spanKind, attributes, parentLinks)) {
                        return SamplingResult.recordAndSample();
                    }
                }
                return SamplingResult.drop();
            }
        }
        return SamplingResult.recordAndSample();
    }

    @Override
    public String getDescription() {
        return "RuleBasedSampler";
    }
}
