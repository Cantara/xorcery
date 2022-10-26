package com.exoreaction.xorcery.service.conductor.test.resource.model;

import com.exoreaction.xorcery.jsonapi.model.Attributes;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.GroupTemplatePatternEvaluator;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;

class GroupTemplatePatternEvaluatorTest {

    @Test
    public void testEval()
    {
        // Given
        ServiceResourceObject sro = new ServiceResourceObject(new ResourceObject.Builder("logappender", "server1")
                .attributes(new Attributes.Builder().attribute("environment", "development").attribute("tag", "sandbox"))
                .build());
        Link link = new Link("logevents", "http://localhost/ws/logevents");
        GroupTemplatePatternEvaluator evaluator = new GroupTemplatePatternEvaluator(sro, link.rel());

        // Then
        assertThat(evaluator.eval("rel=='logevents'"), Matchers.equalTo(true));
        assertThat(evaluator.eval("type=='logappender'"), Matchers.equalTo(true));

        assertThat(evaluator.eval("tag=='sandbox'"), Matchers.equalTo(true));
        assertThat(evaluator.eval("environment=='development'"), Matchers.equalTo(true));
    }
}